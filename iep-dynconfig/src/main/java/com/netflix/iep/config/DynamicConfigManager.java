/*
 * Copyright 2014-2025 Netflix, Inc.
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

/**
 * Base interface for a config manager that allows the base config to be updated with
 * an override layer dynamically at runtime.
 */
public interface DynamicConfigManager {

  /**
   * Create a new instance of a dynamic config manager.
   *
   * @param baseConfig
   *     Base config layer that will be used as a fallback to the dynamic layer.
   * @return
   *     Config manager instance.
   */
  static DynamicConfigManager create(Config baseConfig) {
    return new DynamicConfigManagerImpl(baseConfig);
  }

  /**
   * Returns the current config instance, i.e., override with fallback to the base config.
   */
  Config get();

  /**
   * Set the override config layer.
   */
  void setOverrideConfig(Config override);

  /**
   * Add a listener that will get invoked once when added and then each time the override config
   * layer is updated. When invoked for the initialization, the previous config value will be
   * {@code null}.
   */
  void addListener(ConfigListener listener);

  /**
   * Remove the listener so it will no longer get invoked.
   */
  void removeListener(ConfigListener listener);
}
