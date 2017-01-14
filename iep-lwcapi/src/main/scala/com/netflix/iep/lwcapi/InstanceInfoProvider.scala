/*
 * Copyright 2014-2017 Netflix, Inc.
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
package com.netflix.iep.lwcapi

import javax.inject.Singleton

import com.google.inject.{Inject, Provider}
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider
import com.netflix.appinfo.{EurekaInstanceConfig, InstanceInfo}

/**
  * Created by mgraff on 12/8/16.
  */
@Singleton class InstanceInfoProvider @Inject private(val config: EurekaInstanceConfig) extends Provider[InstanceInfo] {
  final private var infoProvider: EurekaConfigBasedInstanceInfoProvider = new EurekaConfigBasedInstanceInfoProvider(config)

  def get: InstanceInfo = infoProvider.get
}
