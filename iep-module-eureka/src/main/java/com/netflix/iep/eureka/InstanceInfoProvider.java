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
package com.netflix.iep.eureka;

import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Wraps EurekaConfigBasedInstanceInfoProvider to avoid scope problems with governator classes
 * used in eureka.
 */
@Singleton
class InstanceInfoProvider implements Provider<InstanceInfo> {

  private final EurekaConfigBasedInstanceInfoProvider infoProvider;

  @Inject
  InstanceInfoProvider(EurekaInstanceConfig config) {
    infoProvider = new EurekaConfigBasedInstanceInfoProvider(config);
  }

  @Override public InstanceInfo get() {
    return infoProvider.get();
  }
}
