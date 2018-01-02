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

import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.iep.service.ServiceManager;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Healthcheck handler that ties the eureka state to the services that are registered with the
 * manager.
 */
public class EurekaHandler implements HealthCheckHandler {

  private final Provider<ServiceManager> manager;

  @Inject
  public EurekaHandler(Provider<ServiceManager> manager) {
    this.manager = manager;
  }

  @Override
  public InstanceInfo.InstanceStatus getStatus(InstanceInfo.InstanceStatus current) {
    final boolean healthy = manager.get().isHealthy();
    return healthy ? InstanceInfo.InstanceStatus.UP : InstanceInfo.InstanceStatus.DOWN;
  }
}
