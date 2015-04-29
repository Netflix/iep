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
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConcurrentMapConfiguration;
import com.netflix.config.ConfigurationManager;
import org.apache.commons.configuration.Configuration;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
class ConfigProvider implements Provider<Configuration> {

  @Inject
  ConfigProvider(Config config) {
    Configuration v1config = ConfigurationManager.getConfigInstance();
    ConcurrentMapConfiguration override = new ConcurrentMapConfiguration();
    if (v1config instanceof ConcurrentCompositeConfiguration) {
      ((ConcurrentCompositeConfiguration) v1config).addConfigurationAtFront(override, "override");
    }

    Archaius2Listener listener = new Archaius2Listener(override, config);
    config.addListener(listener);
    listener.initialize();
  }

  @Override public Configuration get() {
    return ConfigurationManager.getConfigInstance();
  }
}
