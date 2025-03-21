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
package com.netflix.iep.admin.endpoints;

import com.netflix.iep.admin.HttpEndpoint;
import com.netflix.iep.service.Service;
import com.netflix.iep.service.ServiceManager;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Indicates the status of services registered with {@link ServiceManager}.
 */
public class ServicesEndpoint implements HttpEndpoint {

  private final ServiceManager manager;

  public ServicesEndpoint(ServiceManager manager) {
    this.manager = manager;
  }

  @Override public Object get() {
    return manager.services()
        .stream()
        .map(this::toMap)
        .collect(Collectors.toList());
  }

  @Override public Object get(String path) {
    return null;
  }

  private Map<String, Object> toMap(Service service) {
    Map<String, Object> map = new TreeMap<>();
    map.put("healthy", service.isHealthy());
    map.put("name",    service.name());
    map.put("state",   service.state().name());
    return map;
  }
}
