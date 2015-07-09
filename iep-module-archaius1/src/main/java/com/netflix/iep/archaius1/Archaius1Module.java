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
import com.google.inject.Provides;
import com.netflix.archaius.bridge.StaticArchaiusBridgeModule;
import com.netflix.config.ConfigurationManager;
import org.apache.commons.configuration.Configuration;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Helper for configuring archaius v1.
 */
public final class Archaius1Module extends AbstractModule {

  @Override protected void configure() {
    // It is assumed that some other module will take care of configuring archaius2.
    install(new StaticArchaiusBridgeModule());
  }

  @Provides
  @Singleton
  @Named("IEP")
  private Configuration providesConfiguration() {
    return ConfigurationManager.getConfigInstance();
  }

  @Override public boolean equals(Object obj) {
    return obj != null && getClass().equals(obj.getClass());
  }

  @Override public int hashCode() {
    return getClass().hashCode();
  }
}
