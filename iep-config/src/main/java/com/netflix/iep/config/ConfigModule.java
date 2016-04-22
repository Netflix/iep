/*
 * Copyright 2014-2016 Netflix, Inc.
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

import com.google.inject.Key;
import com.google.inject.Provides;
import com.netflix.archaius.api.config.CompositeConfig;
import com.netflix.archaius.api.inject.RemoteLayer;
import com.netflix.archaius.config.DefaultCompositeConfig;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.archaius.typesafe.TypesafeConfig;
import com.netflix.iep.admin.AdminModule;
import com.netflix.iep.platformservice.ApplicationLayer;
import com.netflix.iep.platformservice.PlatformServiceModule;
import com.netflix.iep.platformservice.PropsEndpoint;
import com.netflix.spectator.api.Spectator;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import javax.inject.Singleton;
import java.util.Properties;

public class ConfigModule extends ArchaiusModule {

  private final String[] propFiles;

  public ConfigModule() {
    this(null);
  }

  public ConfigModule(String[] propFiles) {
    this.propFiles = propFiles;
  }

  @Override protected void configureArchaius() {
    install(new AdminModule());
    AdminModule.endpointsBinder(binder()).addBinding("/props").to(PropsEndpoint.class);
    bindApplicationConfigurationOverride()
        .to(Key.get(com.netflix.archaius.api.Config.class, ApplicationLayer.class));
    bind(DynamicPropertiesConfiguration.class).asEagerSingleton();
  }

  @Override public boolean equals(Object obj) {
    return obj != null && getClass().equals(obj.getClass());
  }

  @Override public int hashCode() {
    return getClass().hashCode();
  }

  @Provides
  @Singleton
  Config providesTypesafeConfig() {
    final String prop = "netflix.iep.env.account-type";
    final Config baseConfig = ConfigFactory.load();
    final String envConfigName = "iep-" + baseConfig.getString(prop) + ".conf";
    final Config envConfig = ConfigFactory.parseResources(envConfigName);
    return envConfig.withFallback(baseConfig).resolve();
  }

  @Provides
  @Singleton
  @RemoteLayer
  private com.netflix.archaius.api.Config providesOverrideConfig(Config cfg) throws Exception {
    return PlatformServiceModule.getDynamicConfig(Spectator.globalRegistry(), cfg);
  }

  @Provides
  @Singleton
  @ApplicationLayer
  private com.netflix.archaius.api.Config providesAppConfig(Config cfg) throws Exception {
    final Properties props = (propFiles == null)
        ? ScopedPropertiesLoader.load()
        : ScopedPropertiesLoader.load(propFiles);
    final CompositeConfig app = new DefaultCompositeConfig();
    app.addConfig("scoped", new MapConfig(props));
    app.addConfig("typesafe", new TypesafeConfig(cfg));
    return app;
  }
}
