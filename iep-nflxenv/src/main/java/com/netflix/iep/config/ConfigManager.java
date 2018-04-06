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
package com.netflix.iep.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Helper for loading the typesafe config instance. In most cases for apps using the IEP
 * libraries this should be used instead of {@link ConfigFactory}. It supports loading
 * additional configuration files based on the context via the {@code netflix.iep.include}
 * setting.
 */
public final class ConfigManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);

  private static final Config CONFIG = load();

  /** Get a cached copy of the config loaded from the default class loader. */
  public static Config get() {
    return CONFIG;
  }

  /** Load config using the default class loader. */
  public static Config load() {
    return load(pickClassLoader());
  }

  /** Load config using the specified class loader. */
  public static Config load(ClassLoader classLoader) {
    final String prop = "netflix.iep.env.account-type";
    final Config baseConfig = ConfigFactory.load(classLoader);
    final String envConfigName = "iep-" + baseConfig.getString(prop) + ".conf";
    final Config envConfig = loadConfigByName(classLoader, envConfigName);
    return loadIncludes(classLoader, envConfig.withFallback(baseConfig).resolve());
  }

  private static ClassLoader pickClassLoader() {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) {
      LOGGER.warn(
          "Thread.currentThread().getContextClassLoader() is null, using loader for {}",
          ConfigManager.class.getName());
      return ConfigManager.class.getClassLoader();
    } else {
      return cl;
    }
  }

  private static Config loadConfigByName(ClassLoader classLoader, String name) {
    LOGGER.debug("loading config {}", name);
    if (name.startsWith("file:")) {
      File f = new File(name.substring("file:".length()));
      return ConfigFactory.parseFile(f);
    } else {
      return ConfigFactory.parseResources(classLoader, name);
    }
  }

  private static Config loadIncludes(ClassLoader classLoader, Config baseConfig) {
    final String prop = "netflix.iep.include";
    Config acc = baseConfig;
    for (String name : baseConfig.getStringList(prop)) {
      Config cfg = loadConfigByName(classLoader, name);
      acc = cfg.withFallback(acc);
    }
    return acc.resolve();
  }
}
