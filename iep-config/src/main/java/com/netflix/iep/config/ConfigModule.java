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
import com.netflix.archaius.AppConfig;
import com.netflix.archaius.DefaultAppConfig;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.archaius.typesafe.TypesafeConfig;
import com.netflix.iep.platformservice.PlatformServiceModule;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import javax.inject.Singleton;
import java.util.Properties;

public class ConfigModule extends AbstractModule {

  private final String[] propFiles;

  public ConfigModule() {
    this(null);
  }

  public ConfigModule(String[] propFiles) {
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
      bind(AppConfig.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    protected AppConfig createAppConfig(Config root) throws Exception {
      final Properties props = (propFiles == null)
          ? ScopedPropertiesLoader.load()
          : ScopedPropertiesLoader.load(propFiles);
      final AppConfig config = DefaultAppConfig.builder()
          .withApplicationConfigName("application")
          .build();
      config.addLibraryConfig(new TypesafeConfig(root.origin().filename(), root));
      config.addLibraryConfig(new MapConfig("scoped", props));
      config.addOverrideConfig(PlatformServiceModule.getDynamicConfig(root));
      return config;
    }
  }
}
