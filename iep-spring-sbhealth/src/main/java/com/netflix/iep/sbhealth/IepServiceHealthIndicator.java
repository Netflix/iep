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
package com.netflix.iep.sbhealth;

import com.netflix.iep.service.Service;
import com.netflix.iep.service.ServiceManager;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Helper to reflect ServiceManager status as a Spring Boot health indicator.
 */
@Component
public class IepServiceHealthIndicator implements HealthIndicator {

  private final Supplier<ServiceManager> serviceManagerSupplier;

  public IepServiceHealthIndicator(Supplier<ServiceManager> serviceManagerSupplier) {
    this.serviceManagerSupplier = serviceManagerSupplier;
  }

  @Override
  public Health health() {
    ServiceManager sm = serviceManagerSupplier.get();
    return sm.isHealthy()
        ? Health.up().withDetails(createDetails(sm)).build()
        : Health.down().withDetails(createDetails(sm)).build();
  }

  private Map<String, String> createDetails(ServiceManager sm) {
    return sm.services().stream().collect(Collectors.toMap(
        Service::name,
        s -> s.state().name()
    ));
  }
}
