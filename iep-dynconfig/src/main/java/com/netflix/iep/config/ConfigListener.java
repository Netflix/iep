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
import com.typesafe.config.ConfigMemorySize;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.function.BiFunction;
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
      Config c1 = (path == null) ? previous : ListenerUtils.getConfig(previous, path);
      Config c2 = (path == null) ? current : ListenerUtils.getConfig(current, path);
      if (ListenerUtils.hasChanged(c1, c2)) {
        consumer.accept(c2);
      }
    };
  }

  /**
   * Create a listener instance that will invoke the consumer when an update causes a
   * change for the specified property.
   *
   * @param property
   *     Property to get from the config.
   * @param consumer
   *     Handler that will get invoked if the update resulted in a change for the property.
   * @return
   *     Listener instance that forwards changes for the property to the consumer.
   */
  static ConfigListener forConfig(String property, Consumer<Config> consumer) {
    return forConfigEntry(property, consumer, Config::getConfig);
  }

  /**
   * Create a listener instance that will invoke the consumer when an update causes a
   * change for the specified property.
   *
   * @param property
   *     Property to get from the config.
   * @param consumer
   *     Handler that will get invoked if the update resulted in a change for the property.
   * @return
   *     Listener instance that forwards changes for the property to the consumer.
   */
  static ConfigListener forConfigList(String property, Consumer<List<? extends Config>> consumer) {
    return forConfigEntry(property, consumer, Config::getConfigList);
  }

  /**
   * Create a listener instance that will invoke the consumer when an update causes a
   * change for the specified property.
   *
   * @param property
   *     Property to get from the config.
   * @param consumer
   *     Handler that will get invoked if the update resulted in a change for the property.
   * @return
   *     Listener instance that forwards changes for the property to the consumer.
   */
  static ConfigListener forString(String property, Consumer<String> consumer) {
    return forConfigEntry(property, consumer, Config::getString);
  }

  /**
   * Create a listener instance that will invoke the consumer when an update causes a
   * change for the specified property.
   *
   * @param property
   *     Property to get from the config.
   * @param consumer
   *     Handler that will get invoked if the update resulted in a change for the property.
   * @return
   *     Listener instance that forwards changes for the property to the consumer.
   */
  static ConfigListener forStringList(String property, Consumer<List<String>> consumer) {
    return forConfigEntry(property, consumer, Config::getStringList);
  }

  /**
   * Create a listener instance that will invoke the consumer when an update causes a
   * change for the specified property.
   *
   * @param property
   *     Property to get from the config.
   * @param consumer
   *     Handler that will get invoked if the update resulted in a change for the property.
   * @return
   *     Listener instance that forwards changes for the property to the consumer.
   */
  static ConfigListener forBoolean(String property, Consumer<Boolean> consumer) {
    return forConfigEntry(property, consumer, Config::getBoolean);
  }

  /**
   * Create a listener instance that will invoke the consumer when an update causes a
   * change for the specified property.
   *
   * @param property
   *     Property to get from the config.
   * @param consumer
   *     Handler that will get invoked if the update resulted in a change for the property.
   * @return
   *     Listener instance that forwards changes for the property to the consumer.
   */
  static ConfigListener forBooleanList(String property, Consumer<List<Boolean>> consumer) {
    return forConfigEntry(property, consumer, Config::getBooleanList);
  }

  /**
   * Create a listener instance that will invoke the consumer when an update causes a
   * change for the specified property.
   *
   * @param property
   *     Property to get from the config.
   * @param consumer
   *     Handler that will get invoked if the update resulted in a change for the property.
   * @return
   *     Listener instance that forwards changes for the property to the consumer.
   */
  static ConfigListener forInt(String property, Consumer<Integer> consumer) {
    return forConfigEntry(property, consumer, Config::getInt);
  }

  /**
   * Create a listener instance that will invoke the consumer when an update causes a
   * change for the specified property.
   *
   * @param property
   *     Property to get from the config.
   * @param consumer
   *     Handler that will get invoked if the update resulted in a change for the property.
   * @return
   *     Listener instance that forwards changes for the property to the consumer.
   */
  static ConfigListener forIntList(String property, Consumer<List<Integer>> consumer) {
    return forConfigEntry(property, consumer, Config::getIntList);
  }

  /**
   * Create a listener instance that will invoke the consumer when an update causes a
   * change for the specified property.
   *
   * @param property
   *     Property to get from the config.
   * @param consumer
   *     Handler that will get invoked if the update resulted in a change for the property.
   * @return
   *     Listener instance that forwards changes for the property to the consumer.
   */
  static ConfigListener forLong(String property, Consumer<Long> consumer) {
    return forConfigEntry(property, consumer, Config::getLong);
  }

  /**
   * Create a listener instance that will invoke the consumer when an update causes a
   * change for the specified property.
   *
   * @param property
   *     Property to get from the config.
   * @param consumer
   *     Handler that will get invoked if the update resulted in a change for the property.
   * @return
   *     Listener instance that forwards changes for the property to the consumer.
   */
  static ConfigListener forLongList(String property, Consumer<List<Long>> consumer) {
    return forConfigEntry(property, consumer, Config::getLongList);
  }

  /**
   * Create a listener instance that will invoke the consumer when an update causes a
   * change for the specified property.
   *
   * @param property
   *     Property to get from the config.
   * @param consumer
   *     Handler that will get invoked if the update resulted in a change for the property.
   * @return
   *     Listener instance that forwards changes for the property to the consumer.
   */
  static ConfigListener forBytes(String property, Consumer<Long> consumer) {
    return forConfigEntry(property, consumer, Config::getBytes);
  }

  /**
   * Create a listener instance that will invoke the consumer when an update causes a
   * change for the specified property.
   *
   * @param property
   *     Property to get from the config.
   * @param consumer
   *     Handler that will get invoked if the update resulted in a change for the property.
   * @return
   *     Listener instance that forwards changes for the property to the consumer.
   */
  static ConfigListener forBytesList(String property, Consumer<List<Long>> consumer) {
    return forConfigEntry(property, consumer, Config::getBytesList);
  }

  /**
   * Create a listener instance that will invoke the consumer when an update causes a
   * change for the specified property.
   *
   * @param property
   *     Property to get from the config.
   * @param consumer
   *     Handler that will get invoked if the update resulted in a change for the property.
   * @return
   *     Listener instance that forwards changes for the property to the consumer.
   */
  static ConfigListener forMemorySize(String property, Consumer<ConfigMemorySize> consumer) {
    return forConfigEntry(property, consumer, Config::getMemorySize);
  }

  /**
   * Create a listener instance that will invoke the consumer when an update causes a
   * change for the specified property.
   *
   * @param property
   *     Property to get from the config.
   * @param consumer
   *     Handler that will get invoked if the update resulted in a change for the property.
   * @return
   *     Listener instance that forwards changes for the property to the consumer.
   */
  static ConfigListener forMemorySizeList(String property, Consumer<List<ConfigMemorySize>> consumer) {
    return forConfigEntry(property, consumer, Config::getMemorySizeList);
  }

  /**
   * Create a listener instance that will invoke the consumer when an update causes a
   * change for the specified property.
   *
   * @param property
   *     Property to get from the config.
   * @param consumer
   *     Handler that will get invoked if the update resulted in a change for the property.
   * @return
   *     Listener instance that forwards changes for the property to the consumer.
   */
  static ConfigListener forDouble(String property, Consumer<Double> consumer) {
    return forConfigEntry(property, consumer, Config::getDouble);
  }

  /**
   * Create a listener instance that will invoke the consumer when an update causes a
   * change for the specified property.
   *
   * @param property
   *     Property to get from the config.
   * @param consumer
   *     Handler that will get invoked if the update resulted in a change for the property.
   * @return
   *     Listener instance that forwards changes for the property to the consumer.
   */
  static ConfigListener forDoubleList(String property, Consumer<List<Double>> consumer) {
    return forConfigEntry(property, consumer, Config::getDoubleList);
  }

  /**
   * Create a listener instance that will invoke the consumer when an update causes a
   * change for the specified property.
   *
   * @param property
   *     Property to get from the config.
   * @param consumer
   *     Handler that will get invoked if the update resulted in a change for the property.
   * @return
   *     Listener instance that forwards changes for the property to the consumer.
   */
  static ConfigListener forNumber(String property, Consumer<Number> consumer) {
    return forConfigEntry(property, consumer, Config::getNumber);
  }

  /**
   * Create a listener instance that will invoke the consumer when an update causes a
   * change for the specified property.
   *
   * @param property
   *     Property to get from the config.
   * @param consumer
   *     Handler that will get invoked if the update resulted in a change for the property.
   * @return
   *     Listener instance that forwards changes for the property to the consumer.
   */
  static ConfigListener forNumberList(String property, Consumer<List<Number>> consumer) {
    return forConfigEntry(property, consumer, Config::getNumberList);
  }

  /**
   * Create a listener instance that will invoke the consumer when an update causes a
   * change for the specified property.
   *
   * @param property
   *     Property to get from the config.
   * @param consumer
   *     Handler that will get invoked if the update resulted in a change for the property.
   * @return
   *     Listener instance that forwards changes for the property to the consumer.
   */
  static ConfigListener forDuration(String property, Consumer<Duration> consumer) {
    return forConfigEntry(property, consumer, Config::getDuration);
  }

  /**
   * Create a listener instance that will invoke the consumer when an update causes a
   * change for the specified property.
   *
   * @param property
   *     Property to get from the config.
   * @param consumer
   *     Handler that will get invoked if the update resulted in a change for the property.
   * @return
   *     Listener instance that forwards changes for the property to the consumer.
   */
  static ConfigListener forDurationList(String property, Consumer<List<Duration>> consumer) {
    return forConfigEntry(property, consumer, Config::getDurationList);
  }

  /**
   * Create a listener instance that will invoke the consumer when an update causes a
   * change for the specified property.
   *
   * @param property
   *     Property to get from the config.
   * @param consumer
   *     Handler that will get invoked if the update resulted in a change for the property.
   * @return
   *     Listener instance that forwards changes for the property to the consumer.
   */
  static ConfigListener forPeriod(String property, Consumer<Period> consumer) {
    return forConfigEntry(property, consumer, Config::getPeriod);
  }

  /**
   * Create a listener instance that will invoke the consumer when an update causes a
   * change for the specified property.
   *
   * @param property
   *     Property to get from the config.
   * @param consumer
   *     Handler that will get invoked if the update resulted in a change for the property.
   * @return
   *     Listener instance that forwards changes for the property to the consumer.
   */
  static ConfigListener forTemporal(String property, Consumer<TemporalAmount> consumer) {
    return forConfigEntry(property, consumer, Config::getTemporal);
  }

  /**
   * Create a listener instance that will invoke the consumer when an update causes a
   * change for the specified property.
   *
   * @param property
   *     Property to get from the config.
   * @param consumer
   *     Handler that will get invoked if the update resulted in a change for the property.
   * @param accessor
   *     Function used to access the property value from the config.
   * @return
   *     Listener instance that forwards changes for the property to the consumer.
   */
  static <T> ConfigListener forConfigEntry(
      String property,
      Consumer<T> consumer,
      BiFunction<Config, String, T> accessor) {
    if (property == null) {
      throw new NullPointerException("property cannot be null");
    }
    return (previous, current) -> {
      T v1 = ListenerUtils.getOrNull(previous, property, accessor);
      T v2 = ListenerUtils.getOrNull(current, property, accessor);
      if (ListenerUtils.hasChanged(v1, v2)) {
        consumer.accept(v2);
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
