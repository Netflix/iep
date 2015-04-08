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

import com.netflix.archaius.config.polling.PollingResponse;
import com.netflix.archaius.persisted2.JsonPersistedV2Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Additional debug logging for the response.
 */
class RemotePropertiesCallable implements Callable<PollingResponse> {

  private static final Logger LOGGER = LoggerFactory.getLogger(RemotePropertiesCallable.class);

  private final String url;
  private final JsonPersistedV2Reader reader;

  RemotePropertiesCallable(String url, JsonPersistedV2Reader reader) {
    this.url = url;
    this.reader = reader;
  }

  @Override public PollingResponse call() throws Exception {
    LOGGER.debug("updating properties from {}", url);
    try {
      PollingResponse response = reader.call();
      if (LOGGER.isDebugEnabled()) {
        if (response.hasData()) {
          for (Map.Entry<String, String> entry : response.getToAdd().entrySet()) {
            LOGGER.debug("received property [{}] = [{}]", entry.getKey(), entry.getValue());
          }
          for (String key : response.getToRemove()) {
            LOGGER.debug("deleted property [{}]", key);
          }
        } else {
          LOGGER.debug("no property changes detected");
        }
      }
      return response;
    } catch (Exception e) {
      LOGGER.debug("failed to get properties", e);
      throw e;
    }
  }
}
