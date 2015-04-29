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
package com.netflix.iep.archaius1;

import com.netflix.archaius.Config;
import com.netflix.archaius.ConfigListener;
import com.netflix.config.AbstractPollingScheduler;
import com.netflix.config.PollResult;
import org.apache.commons.configuration.AbstractConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Listener that updates the override layer of archaius1 when there are changes to the
 * archaius2 config.
 */
class Archaius2Listener extends AbstractPollingScheduler implements ConfigListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(Archaius2Listener.class);

  private final AbstractConfiguration v1config;

  Archaius2Listener(AbstractConfiguration v1config) {
    this.v1config = v1config;
  }

  @Override protected void schedule(Runnable runnable) {
  }

  @Override public void stop() {
  }

  void update(Config config) {
    Map<String, Object> props = new HashMap<>();
    Iterator<String> iter = config.getKeys();
    while (iter.hasNext()) {
      String k = iter.next();
      props.put(k, config.getString(k));
    }
    LOGGER.debug("update received with {} properties", props.size());
    populateProperties(PollResult.createFull(props), v1config);
  }

  @Override public void onConfigAdded(Config config) {
    update(config);
  }

  @Override public void onConfigRemoved(Config config) {
    update(config);
  }

  @Override public void onConfigUpdated(Config config) {
    update(config);
  }

  @Override public void onError(Throwable throwable, Config config) {
  }
}
