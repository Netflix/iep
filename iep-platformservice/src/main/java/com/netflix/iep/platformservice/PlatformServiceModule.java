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
package com.netflix.iep.platformservice;

import com.google.inject.Key;
import com.google.inject.Provides;
import com.netflix.archaius.api.config.PollingStrategy;
import com.netflix.archaius.api.inject.RemoteLayer;
import com.netflix.archaius.config.EmptyConfig;
import com.netflix.archaius.config.polling.PollingResponse;
import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.archaius.typesafe.TypesafeConfig;
import com.netflix.iep.admin.AdminConfig;
import com.netflix.iep.admin.guice.AdminModule;
import com.netflix.iep.config.ConfigManager;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Spectator;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Helper for configuring archaius with the Netflix dynamic property source.
 */
public final class PlatformServiceModule extends ArchaiusModule {

  private static final Logger LOGGER = LoggerFactory.getLogger(PlatformServiceModule.class);

  @Override protected void configureArchaius() {
    bindApplicationConfigurationOverride()
      .to(Key.get(com.netflix.archaius.api.Config.class, ApplicationLayer.class));
    AdminModule.endpointsBinder(binder()).addBinding("/props").to(PropsEndpoint.class);
  }

  @Provides
  @Singleton
  Config providesTypesafeConfig() {
    return ConfigManager.get();
  }

  @Provides
  @Singleton
  @RemoteLayer
  private com.netflix.archaius.api.Config providesOverrideConfig(Config cfg)
      throws Exception {
    return getDynamicConfig(Spectator.globalRegistry(), cfg);
  }

  @Provides
  @Singleton
  @ApplicationLayer
  protected com.netflix.archaius.api.Config providesAppConfig(final Config application) {
    return new TypesafeConfig(application);
  }

  @Provides
  @Singleton
  private AdminConfig providesAdminConfig(Config cfg) {
    return new AdminConfig() {
      @Override public int port() {
        return cfg.getInt("netflix.iep.admin.port");
      }

      @Override public int backlog() {
        return cfg.getInt("netflix.iep.admin.backlog");
      }

      @Override public Duration shutdownDelay() {
        long nanos = cfg.getDuration("netflix.iep.admin.shutdown-delay", TimeUnit.NANOSECONDS);
        return Duration.ofNanos(nanos);
      }

      @Override public String uiLocation() {
        final String k = "netflix.iep.admin.ui-location";
        return cfg.hasPath(k) ? cfg.getString(k) : "/ui";
      }
    };
  }

  @Override public boolean equals(Object obj) {
    return obj != null && getClass().equals(obj.getClass());
  }

  @Override public int hashCode() {
    return getClass().hashCode();
  }

  private static Callable<PollingResponse> getCallback(Registry registry, Config cfg)
      throws Exception {
    final String prop = "netflix.iep.archaius.url";
    final URL url = URI.create(cfg.getString(prop)).toURL();
    return new PropertiesReader(registry, url);
  }

  private static PollingStrategy getPollingStrategy(Config cfg) {
    final String propPollingInterval = "netflix.iep.archaius.polling-interval";
    final String propSyncInit = "netflix.iep.archaius.sync-init";
    final long interval = cfg.getDuration(propPollingInterval, TimeUnit.MILLISECONDS);
    return new FixedPollingStrategy(interval, TimeUnit.MILLISECONDS, cfg.getBoolean(propSyncInit));
  }

  public static com.netflix.archaius.api.Config getDynamicConfig(Registry registry, Config cfg)
      throws Exception {
    final String propUseDynamic = "netflix.iep.archaius.use-dynamic";
    return (cfg.getBoolean(propUseDynamic))
      ? new PollingDynamicConfig(getCallback(registry, cfg), getPollingStrategy(cfg))
      : EmptyConfig.INSTANCE;
  }
}
