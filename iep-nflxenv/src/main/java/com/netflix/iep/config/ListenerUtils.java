/*
 * Copyright 2014-2022 Netflix, Inc.
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

import java.util.function.BiFunction;

/**
 * Helper functions for use with {@link ConfigListener}.
 */
class ListenerUtils {

  private ListenerUtils() {
  }

  static boolean hasChanged(Object previous, Object current) {
    return (previous != null && !previous.equals(current))
        || (previous == null && current != null);
  }

  static <T> T getOrNull(Config config, String path, BiFunction<Config, String, T> accessor) {
    return config != null && config.hasPath(path)
        ? accessor.apply(config, path)
        : null;
  }

  static Config getConfig(Config config, String path) {
    return getOrNull(config, path, Config::getConfig);
  }
}
