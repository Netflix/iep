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

import java.util.function.Consumer;

/**
 * Listener that will get invoked when the config is updated.
 */
@FunctionalInterface
public interface ConfigListener {

  /**
   * Create a listener instance that will invoke the consumer when an update causes a
   * change for the specified path.
   *
   * @param path
   *     Config prefix to get from the config. If null, then the full config will be used.
   * @param consumer
   *     Handler that will get invoked if the update resulted in a change for the specified
   *     path.
   * @return
   *     Listener instance that forwards changes for the path to the consumer.
   */
  static ConfigListener forPath(String path, Consumer<Config> consumer) {
    return (previous, current) -> {
      Config c1 = (path == null) ? previous : previous.getConfig(path);
      Config c2 = (path == null) ? current : current.getConfig(path);
      if (!c1.equals(c2)) {
        consumer.accept(c2);
      }
    };
  }

  /**
   * Invoked when the config is updated by a call to
   * {@link DynamicConfigManager#setOverrideConfig(Config)}. This method will be invoked
   * from the thread setting the override. It should be cheap to allow changes to quickly
   * propagate to all listeners. If an exception is thrown, then a warning will be logged
   * and the manager will move on.
   *
   * @param previous
   *     Previous config instance.
   * @param current
   *     Current config instance with the update override applied over the base layer.
   */
  void onUpdate(Config previous, Config current);
}
