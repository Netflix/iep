/*
 * Copyright 2014-2025 Netflix, Inc.
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
package com.netflix.iep.spectatord;

import com.netflix.iep.service.AbstractService;
import com.netflix.spectator.api.Clock;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Spectator;
import com.netflix.spectator.gc.GcLogger;
import com.netflix.spectator.jvm.Jmx;
import com.netflix.spectator.sidecar.SidecarConfig;
import com.netflix.spectator.sidecar.SidecarRegistry;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for StatelessRegistry that reports to Atlas aggregator endpoint. If using
 * an add on to Guice that supports lifecycle, then the service will get started and
 * stopped by the lifecycle manager. If the post construct method is not called automatically,
 * then the registry will be started when accessed, but never stopped. This means that
 * the final metrics during shutdown may not get reported correctly.
 */
class SidecarRegistryService extends AbstractService {

  private static Config defaultConfig() {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) {
      cl = AtlasSidecarConfiguration.class.getClassLoader();
    }
    return ConfigFactory.load(cl);
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(SidecarRegistryService.class);

  private final Config config;

  private SidecarRegistry registry;
  private GcLogger gcLogger;

  SidecarRegistryService(Config config) {
    this.config = (config == null) ? defaultConfig() : config;
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

    registry = new SidecarRegistry(Clock.SYSTEM, new TypesafeConfig(cfg));

    // Add to global registry for http stats and GC logger
    Spectator.globalRegistry().add(registry);

    // Enable GC logger
    gcLogger = new GcLogger();
    if (cfg.getBoolean("sidecar.collection.gc")) {
      gcLogger.start(null);
    }

    // Enable JVM data collection
    if (cfg.getBoolean("sidecar.collection.jvm")) {
      Jmx.registerStandardMXBeans(registry);
    }
  }

  @Override protected void stopImpl() throws Exception {
    gcLogger.stop();
  }

  private static class TypesafeConfig implements SidecarConfig {

    private final Config config;

    TypesafeConfig(Config config) {
      this.config = config;
    }

    @Override public String get(String k) {
      return config.hasPath(k) ? config.getString(k) : null;
    }
  }
}
