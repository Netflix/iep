/*
 * Copyright 2014-2020 Netflix, Inc.
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
package com.netflix.iep.dynconfig;

import com.netflix.iep.config.ConfigManager;
import com.netflix.iep.service.AbstractService;
import com.netflix.spectator.api.Functions;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.patterns.PolledMeter;
import com.netflix.spectator.ipc.http.HttpClient;
import com.netflix.spectator.ipc.http.HttpResponse;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Polls the remote property service to refresh the set of dynamic properties.
 */
class DynamicConfigService extends AbstractService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigService.class);

  private final AtomicLong lastUpdateTime;
  private final URI uri;
  private final long pollingInterval;
  private final boolean syncInit;
  private final ScheduledExecutorService executor;

  @Inject
  DynamicConfigService(Registry registry) {
    this.lastUpdateTime = PolledMeter.using(registry)
        .withName("iep.archaius.cacheAge")
        .monitorValue(
          new AtomicLong(System.currentTimeMillis()),
          Functions.AGE);

    Config config = ConfigManager.get();
    this.uri = URI.create(config.getString("netflix.iep.archaius.url"));
    this.pollingInterval = config.getDuration("netflix.iep.archaius.polling-interval", TimeUnit.MILLISECONDS);
    this.syncInit = config.getBoolean("netflix.iep.archaius.sync-init");
    this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "DynamicConfigService");
      t.setDaemon(true);
      return t;
    });
  }

  @Override protected void startImpl() throws Exception {
    // Wait until properties have been updated at least once
    while (syncInit) {
      if (update()) {
        break;
      } else {
        Thread.sleep(pollingInterval);
      }
    }

    // Schedule for regular updates
    executor.scheduleWithFixedDelay(this::update, pollingInterval, pollingInterval, TimeUnit.MILLISECONDS);
  }

  @Override protected void stopImpl() throws Exception {
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.MINUTES);
  }

  /**
   * Query remote service and update the properties. Returns true if successful.
   */
  boolean update() {
    try {
      LOGGER.debug("updating properties from {}", uri);

      HttpResponse response = HttpClient.DEFAULT_CLIENT.get(uri).send();

      if (response.status() == 200) {
        try (InputStream in = new ByteArrayInputStream(response.entity())) {
          final Properties props = new Properties();
          props.load(in);

          if (LOGGER.isTraceEnabled()) {
            props.stringPropertyNames().forEach(
                k -> LOGGER.trace("received property: [{}] = [{}]", k, props.getProperty(k)));
          }

          updateDynamicConfig(props);
          lastUpdateTime.set(System.currentTimeMillis());
        }
      } else {
        throw new IOException("request failed with status: " + response.status());
      }

      return true;
    } catch (Exception e) {
      LOGGER.warn("failed to update dynamic properties", e);
      return false;
    }
  }

  /**
   * Update the {@link ConfigManager#dynamicConfigManager()} with the properties. The value
   * for a special key {@code netflix.iep.override} will be treated as a Typesafe Config string
   * so that all constructs can be supported. Other properties will get used directly.
   */
  void updateDynamicConfig(Properties props) {
    final String overrideKey = "netflix.iep.override";
    if (props.containsKey(overrideKey)) {
      Config override = ConfigFactory.parseString(props.getProperty(overrideKey));
      props.remove(overrideKey);
      Config config = override.withFallback(ConfigFactory.parseProperties(props));
      ConfigManager.dynamicConfigManager().setOverrideConfig(config);
    } else {
      Config config = ConfigFactory.parseProperties(props);
      ConfigManager.dynamicConfigManager().setOverrideConfig(config);
    }
  }
}
