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
import org.apache.commons.configuration.Configuration;
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

  private final Configuration v1config;
  private final Config v2config;
  private volatile boolean initialized;

  Archaius2Listener(Configuration v1config, Config v2config) {
    this.v1config = v1config;
    this.v2config = v2config;
  }

  void initialize() {
    initialized = true;
    update();
  }

  @Override protected void schedule(Runnable runnable) {
  }

  @Override public void stop() {
  }

  void update() {
    if (initialized) {
      Map<String, Object> props = new HashMap<>();
      Iterator<String> iter = v2config.getKeys();
      while (iter.hasNext()) {
        String k = iter.next();
        props.put(k, v2config.getString(k));
      }
      LOGGER.debug("update received with {} properties", props.size());
      populateProperties(PollResult.createFull(props), v1config);
    }
  }

  @Override public void onConfigAdded(Config config) {
    update();
  }

  @Override public void onConfigRemoved(Config config) {
    update();
  }

  @Override public void onConfigUpdated(Config config) {
    update();
  }

  @Override public void onError(Throwable throwable, Config config) {
  }
}
