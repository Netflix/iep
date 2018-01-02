/*
 * Copyright 2014-2018 Netflix, Inc.
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
package com.netflix.iep.atlas;

import com.google.inject.Inject;
import com.netflix.iep.service.AbstractService;
import com.netflix.spectator.api.Clock;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Spectator;
import com.netflix.spectator.atlas.AtlasConfig;
import com.netflix.spectator.atlas.AtlasRegistry;
import com.netflix.spectator.gc.GcLogger;
import com.netflix.spectator.jvm.Jmx;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for AtlasRegistry. If using an add on to Guice that supports lifecycle,
 * then the service will get started and stopped by the lifecycle manager. If the
 * post construct method is not called automatically, then the registry will be
 * started when accessed, but never stopped. This means that the final metrics
 * during shutdown may not get reported correctly.
 */
@Singleton
class AtlasRegistryService extends AbstractService {

  private static Config defaultConfig() {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) {
      cl = AtlasModule.class.getClassLoader();
    }
    return ConfigFactory.load(cl);
  }

  private static Logger LOGGER = LoggerFactory.getLogger(AtlasRegistryService.class);

  @Inject(optional = true)
  private Clock clock = Clock.SYSTEM;

  @Inject(optional = true)
  private Config config = defaultConfig();

  private AtlasRegistry registry;
  private GcLogger gcLogger;

  AtlasRegistryService() {
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
    Config cfg = config.getConfig("netflix.iep");

    registry = new AtlasRegistry(clock, new TypesafeAtlasConfig(cfg));
    registry.start();

    // Add to global registry for http stats and GC logger
    Spectator.globalRegistry().add(registry);

    // Enable GC logger
    gcLogger = new GcLogger();
    if (cfg.getBoolean("atlas.collection.gc")) {
      gcLogger.start(null);
    }

    // Enable JVM data collection
    if (cfg.getBoolean("atlas.collection.jvm")) {
      Jmx.registerStandardMXBeans(registry);
    }

    // Start collection for the registry
    registry.start();
  }

  @Override protected void stopImpl() throws Exception {
    gcLogger.stop();
    registry.stop();
  }

  private static class TypesafeAtlasConfig implements AtlasConfig {

    private final Config config;

    TypesafeAtlasConfig(Config config) {
      this.config = config;
    }

    @Override public String get(String k) {
      return config.hasPath(k) ? config.getString(k) : null;
    }

    @Override public Map<String, String> commonTags() {
      Map<String, String> tags = new HashMap<>();
      for (Config cfg : config.getConfigList("atlas.tags")) {
        // These are often populated by environment variables that can sometimes be empty
        // rather than not set when missing. Empty strings are not allowed by Atlas.
        String value = cfg.getString("value");
        if (!value.isEmpty()) {
          tags.put(cfg.getString("key"), cfg.getString("value"));
        }
      }
      return tags;
    }

    @Override public String validTagCharacters() {
      String pattern = get("atlas.validTagCharacters");
      return (pattern == null) ? "-._A-Za-z0-9" : pattern;
    }

    @Override public Map<String, String> validTagValueCharacters() {
      if (config.hasPath("atlas.validTagValueCharacters")) {
        Map<String, String> tags = new HashMap<>();
        for (Config cfg : config.getConfigList("atlas.validTagValueCharacters")) {
          tags.put(cfg.getString("key"), cfg.getString("value"));
        }
        return tags;
      } else {
        return Collections.emptyMap();
      }
    }
  }
}
