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
package com.netflix.iep.config;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.util.Modules;
import com.netflix.archaius.api.config.CompositeConfig;
import com.netflix.archaius.config.DefaultCompositeConfig;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.typesafe.TypesafeConfig;
import com.netflix.iep.platformservice.ApplicationLayer;
import com.netflix.iep.platformservice.PlatformServiceModule;
import com.typesafe.config.Config;

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
    bind(DynamicPropertiesConfiguration.class).asEagerSingleton();
    Module m = Modules.override(new PlatformServiceModule()).with(new OverrideModule());
    install(m);
  }

  private class OverrideModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Override
    public boolean equals(Object obj) {
      return obj != null && getClass().equals(obj.getClass());
    }

    @Override
    public int hashCode() {
      return getClass().hashCode();
    }

    @Provides
    @Singleton
    @ApplicationLayer
    protected com.netflix.archaius.api.Config providesAppConfig(final Config cfg) throws Exception {
      final Properties props = (propFiles == null)
          ? ScopedPropertiesLoader.load()
          : ScopedPropertiesLoader.load(propFiles);
      final CompositeConfig app = new DefaultCompositeConfig();
      app.addConfig("scoped", new MapConfig(props));
      app.addConfig("typesafe", new TypesafeConfig(cfg));
      return app;
    }
  }
}
