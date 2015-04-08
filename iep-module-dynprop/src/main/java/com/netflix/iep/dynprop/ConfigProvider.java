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
package com.netflix.iep.dynprop;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.config.AbstractPollingScheduler;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicConfiguration;
import com.netflix.config.FixedDelayPollingScheduler;
import com.netflix.discovery.DiscoveryClient;
import org.apache.commons.configuration.AbstractConfiguration;

import javax.inject.Inject;
import javax.inject.Provider;

class ConfigProvider implements Provider<AbstractConfiguration> {

  @Inject ConfigProvider(ApplicationInfoManager appInfo, DiscoveryClient client) {
    InstanceInfo info = appInfo.getInfo();
    RemoteConfigurationSource source = new RemoteConfigurationSource(info, client);
    AbstractPollingScheduler scheduler = new FixedDelayPollingScheduler();
    DynamicConfiguration dynamic = new DynamicConfiguration(source, scheduler);
    AbstractConfiguration config = ConfigurationManager.getConfigInstance();
    if (config instanceof ConcurrentCompositeConfiguration) {
      ((ConcurrentCompositeConfiguration) config).addConfigurationAtFront(dynamic, "dynamic");
    }
  }

  @Override public AbstractConfiguration get() {
    return ConfigurationManager.getConfigInstance();
  }
}
