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
package com.netflix.iep.archaius2;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.archaius.AppConfig;
import com.netflix.archaius.DefaultAppConfig;
import com.netflix.archaius.config.PollingDynamicConfig;
import com.netflix.archaius.config.PollingStrategy;
import com.netflix.archaius.config.polling.FixedPollingStrategy;
import com.netflix.archaius.config.polling.PollingResponse;
import com.netflix.archaius.persisted2.JsonPersistedV2Reader;
import com.netflix.archaius.persisted2.ScopePredicates;
import com.netflix.archaius.persisted2.loader.HTTPStreamLoader;
import com.netflix.archaius.typesafe.TypesafeConfig;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;

import javax.inject.Singleton;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Helper for configuring archaius with a dynamic property source.
 */
public class ArchaiusModule extends AbstractModule {
  @Override protected void configure() {
    bind(Config.class).toInstance(ConfigFactory.load());
  }

  private Map<String, String> getScope(Config cfg) {
    final String prop = "netflix.iep.archaius.scope";
    final Config scopeCfg = cfg.getConfig(prop);
    final Map<String, String> scope = new HashMap<>();
    for (Map.Entry<String, ConfigValue> entry : scopeCfg.entrySet()) {
      scope.put(entry.getKey(), entry.getValue().unwrapped().toString());
    }
    return scope;
  }

  private Callable<PollingResponse> getCallback(Config cfg) throws MalformedURLException {
    final String prop = "netflix.iep.archaius.url";
    final URL url = URI.create(cfg.getString(prop)).toURL();
    return JsonPersistedV2Reader.builder(new HTTPStreamLoader(url))
        .withPredicate(new NoGlobalPredicate(ScopePredicates.fromMap(getScope(cfg))))
        .build();
  }

  private PollingStrategy getPollingStrategy(Config cfg) {
    final String prop = "netflix.iep.archaius.polling-interval";
    final long interval = cfg.getDuration(prop, TimeUnit.MILLISECONDS);
    return new FixedPollingStrategy(interval, TimeUnit.MILLISECONDS);
  }

  private PollingDynamicConfig getDynamicConfig(Config cfg) throws MalformedURLException {
    return new PollingDynamicConfig("dynamic", getCallback(cfg), getPollingStrategy(cfg));
  }

  @Provides @Singleton
  AppConfig getAppConfig(Config root) throws Exception {
    final AppConfig config = DefaultAppConfig.builder()
        .withApplicationConfigName("application")
        .build();
    config.addOverrideConfig(new TypesafeConfig(root.origin().filename(), root));
    config.addOverrideConfig(getDynamicConfig(root));
    return config;
  }
}
