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

import com.netflix.archaius.config.polling.PollingResponse;
import com.netflix.spectator.api.Functions;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.sandbox.HttpLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
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
    this.lastUpdateTime = registry.gauge(
        "iep.archaius.cacheAge",
        new AtomicLong(System.currentTimeMillis()),
        Functions.AGE);
    this.url = url;
  }

  @Override public PollingResponse call() throws Exception {
    LOGGER.debug("updating properties from {}", url);

    HttpLogEntry entry = new HttpLogEntry()
        .withClientName("iep-archaius")
        .withMethod("GET")
        .withRequestUri(url.toURI())
        .mark("start");

    HttpURLConnection client = null;
    int status = -1;
    try {
      client = (HttpURLConnection) url.openConnection();
      status = client.getResponseCode();
      entry.withStatusCode(status);
      entry.withStatusReason(client.getResponseMessage());
      client.getHeaderFields().forEach((k, vs) -> {
        // The status line, e.g. [HTTP/1.1 200 OK], is reported as a header with a null key value
        if (k != null) {
          vs.forEach(v -> entry.withResponseHeader(k, v));
        }
      });

      if (status == 200) {
        try (InputStream in = client.getInputStream()) {
          entry.withResponseContentLength(in.available());
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

          lastUpdateTime.set(System.currentTimeMillis());
          return PollingResponse.forSnapshot(data);
        }
      } else {
        throw new IOException("request failed with status: " + status);
      }
    } catch (Exception e) {
      if (status == -1) {
        entry.withException(e);
      }
      throw e;
    } finally {
      entry.mark("end");
      HttpLogEntry.logClientRequest(entry);
      if (client != null) {
        client.disconnect();
      }
    }
  }
}
