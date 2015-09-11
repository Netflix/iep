/*
 * Copyright 2015 Netflix, Inc.
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
package com.netflix.iep.config;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.util.Modules;
import com.netflix.archaius.config.PollingDynamicConfig;
import com.netflix.archaius.config.polling.FixedPollingStrategy;
import com.netflix.archaius.config.polling.PollingResponse;
import com.netflix.archaius.config.CompositeConfig;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.archaius.inject.ApplicationLayer;
import com.netflix.archaius.inject.RemoteLayer;
import com.netflix.archaius.typesafe.TypesafeConfig;
import com.netflix.iep.platformservice.PlatformServiceModule;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import javax.inject.Singleton;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class DynamicNoOpConfigModule extends AbstractModule {

  private final String[] propFiles;

  public DynamicNoOpConfigModule() {
    this(null);
  }

  public DynamicNoOpConfigModule(String[] propFiles) {
    this.propFiles = propFiles;
  }

  @Override protected void configure() {
    Module m = Modules
        .override(new ArchaiusModule())
        .with(new OverrideModule(propFiles));
    install(m);
  }

  private static class OverrideModule extends AbstractModule {

    private final String[] propFiles;

    OverrideModule(String[] propFiles) {
      this.propFiles = propFiles;
    }

    @Override protected void configure() {
      bind(Config.class).toInstance(ConfigFactory.load());
      bind(DynamicPropertiesConfiguration.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    @RemoteLayer
    private com.netflix.archaius.Config providesOverrideConfig(Config cfg) throws Exception {
      return new PollingDynamicConfig(
        new Callable() {
          @Override
          public PollingResponse call() {
            return PollingResponse.noop();
          }
        },
        new FixedPollingStrategy(60000, TimeUnit.MILLISECONDS)
      );
    }

    @Provides
    @Singleton
    @ApplicationLayer
    protected CompositeConfig providesAppConfig(Config cfg) throws Exception {
      final Properties props = (propFiles == null)
          ? ScopedPropertiesLoader.load()
          : ScopedPropertiesLoader.load(propFiles);
      final CompositeConfig app = new CompositeConfig();
      app.addConfig("scoped", new MapConfig(props));
      app.addConfig("typesafe", new TypesafeConfig(cfg));
      return app;
    }
  }
}
