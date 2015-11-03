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
package com.netflix.iep.platformservice;

import com.netflix.archaius.config.polling.PollingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

class PropertiesReader implements Callable<PollingResponse> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesReader.class);

  private final URL url;

  PropertiesReader(URL url) {
    this.url = url;
  }

  @Override public PollingResponse call() throws Exception {
    LOGGER.debug("updating properties from {}", url);
    try (InputStream in = url.openStream()) {
      final Properties props = new Properties();
      props.load(in);
      if (LOGGER.isTraceEnabled()) {
        props.stringPropertyNames().forEach(k -> {
          LOGGER.trace("received property: [{}] = [{}]", k, props.getProperty(k));
        });
      }
      Map<String, String> data = props.stringPropertyNames()
          .stream()
          .collect(Collectors.toMap(k -> k, props::getProperty));
      return PollingResponse.forSnapshot(data);
    }
  }
}
