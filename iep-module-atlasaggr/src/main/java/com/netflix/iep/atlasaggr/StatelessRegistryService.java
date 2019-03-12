/*
 * Copyright 2014-2019 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.iep.atlasaggr;

import com.google.inject.Inject;
import com.netflix.iep.service.AbstractService;
import com.netflix.spectator.api.Clock;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Spectator;
import com.netflix.spectator.gc.GcLogger;
import com.netflix.spectator.jvm.Jmx;
import com.netflix.spectator.stateless.StatelessConfig;
import com.netflix.spectator.stateless.StatelessRegistry;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for StatelessRegistry that reports to Atlas aggregator endpoint. If using
 * an add on to Guice that supports lifecycle, then the service will get started and
 * stopped by the lifecycle manager. If the post construct method is not called automatically,
 * then the registry will be started when accessed, but never stopped. This means that
 * the final metrics during shutdown may not get reported correctly.
 */
@Singleton
class StatelessRegistryService extends AbstractService {

  private static Config defaultConfig() {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) {
      cl = AtlasAggrModule.class.getClassLoader();
    }
    return ConfigFactory.load(cl);
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(StatelessRegistryService.class);

  @Inject(optional = true)
  private Clock clock = Clock.SYSTEM;

  @Inject(optional = true)
  private Config config = defaultConfig();

  private StatelessRegistry registry;
  private GcLogger gcLogger;

  StatelessRegistryService() {
  }

  Registry getRegistry() {
    if (registry == null) {
      LOGGER.info("DI framework does not support @PostConstruct, attempting to start service");
      try {
        start();
      } catch (Exception e) {
        throw new IllegalStateException("failed to start registry service", e);
      }
    }
    return registry;
  }

  @Override protected void startImpl() throws Exception {
    Config cfg = config.getConfig("netflix.iep.atlas");

    registry = new StatelessRegistry(clock, new TypesafeConfig(cfg));
    registry.start();

    // Add to global registry for http stats and GC logger
    Spectator.globalRegistry().add(registry);

    // Enable GC logger
    gcLogger = new GcLogger();
    if (cfg.getBoolean("stateless.collection.gc")) {
      gcLogger.start(null);
    }

    // Enable JVM data collection
    if (cfg.getBoolean("stateless.collection.jvm")) {
      Jmx.registerStandardMXBeans(registry);
    }

    // Start collection for the registry
    registry.start();
  }

  @Override protected void stopImpl() throws Exception {
    gcLogger.stop();
    registry.stop();
  }

  private static class TypesafeConfig implements StatelessConfig {

    private final Config config;

    TypesafeConfig(Config config) {
      this.config = config;
    }

    @Override public String get(String k) {
      return config.hasPath(k) ? config.getString(k) : null;
    }

    @Override public Map<String, String> commonTags() {
      Map<String, String> tags = new HashMap<>();
      for (Config cfg : config.getConfigList("stateless.tags")) {
        // These are often populated by environment variables that can sometimes be empty
        // rather than not set when missing. Empty strings are not allowed by Atlas.
        String value = cfg.getString("value");
        if (!value.isEmpty()) {
          tags.put(cfg.getString("key"), cfg.getString("value"));
        }
      }
      return tags;
    }
  }
}
