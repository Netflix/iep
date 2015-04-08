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

import com.google.inject.AbstractModule;
import com.netflix.config.ConfigurationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Helper for configuring archaius v1.
 */
public class ArchaiusModule extends AbstractModule {

  private static final Logger LOGGER = LoggerFactory.getLogger(ArchaiusModule.class);

  public static void loadProperties(String name) {
    try {
      ConfigurationManager.loadCascadedPropertiesFromResources(name);
    } catch (IOException e) {
      LOGGER.warn("failed to load properties for '" + name + "'");
    }
  }

  @Override protected void configure() {
    loadProperties("application");
  }
}
