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
package com.netflix.iep.config;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of the dynamic config manager interface.
 */
final class DynamicConfigManagerImpl implements DynamicConfigManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigManagerImpl.class);

  private final Config baseConfig;
  private volatile Config current;

  private final Set<ConfigListener> listeners = ConcurrentHashMap.newKeySet();

  /** Create a new instance. */
  DynamicConfigManagerImpl(Config baseConfig) {
    this.baseConfig = baseConfig;
    this.current = baseConfig;
  }

  @Override
  public Config get() {
    return current;
  }

  @Override
  public synchronized void setOverrideConfig(Config override) {
    Config previous = current;
    current = override.withFallback(baseConfig).resolve();
    listeners.forEach(listener -> invokeListener(listener, previous, current));
  }

  private void invokeListener(ConfigListener listener, Config previous, Config current) {
    try {
      listener.onUpdate(previous, current);
    } catch (Exception e) {
      e.printStackTrace();
      LOGGER.warn("failed to update a listener", e);
    }
  }

  @Override
  public void addListener(ConfigListener listener) {
    listeners.add(listener);
    invokeListener(listener, null, current);
  }

  @Override
  public void removeListener(ConfigListener listener) {
    listeners.remove(listener);
  }
}
