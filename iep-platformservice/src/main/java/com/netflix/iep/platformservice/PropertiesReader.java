/*
 * Copyright 2014-2021 Netflix, Inc.
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

import com.netflix.archaius.config.polling.PollingResponse;
import com.netflix.iep.config.ConfigManager;
import com.netflix.spectator.api.Functions;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.patterns.PolledMeter;
import com.netflix.spectator.ipc.http.HttpClient;
import com.netflix.spectator.ipc.http.HttpResponse;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

class PropertiesReader implements Callable<PollingResponse> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesReader.class);

  private final AtomicLong lastUpdateTime;
  private final URL url;

  PropertiesReader(Registry registry, URL url) {
    this.lastUpdateTime = PolledMeter.using(registry)
        .withName("iep.archaius.cacheAge")
        .monitorValue(
          new AtomicLong(System.currentTimeMillis()),
          Functions.AGE);
    this.url = url;
  }

  @Override public PollingResponse call() throws Exception {
    LOGGER.debug("updating properties from {}", url);

    HttpResponse response = HttpClient.DEFAULT_CLIENT
        .get(url.toURI())
        .send();

    if (response.status() == 200) {
      try (InputStream in = new ByteArrayInputStream(response.entity())) {
        final Properties props = new Properties();
        props.load(in);

        if (LOGGER.isTraceEnabled()) {
          props.stringPropertyNames().forEach(
              k -> LOGGER.trace("received property: [{}] = [{}]", k, props.getProperty(k)));
        }

        updateDynamicConfig(props);

        Map<String, String> data = props.stringPropertyNames()
            .stream()
            .collect(Collectors.toMap(k -> k, props::getProperty));

        lastUpdateTime.set(System.currentTimeMillis());
        return PollingResponse.forSnapshot(data);
      }
    } else {
      throw new IOException("request failed with status: " + response.status());
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
