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
package com.netflix.iep.admin.endpoints;

import com.netflix.iep.admin.HttpEndpoint;

import java.util.SortedSet;

/**
 * List all resources available on the admin. This endpoint will get added automatically
 * by {@link com.netflix.iep.admin.AdminServer} to {@code /resources}.
 */
public class ResourcesEndpoint implements HttpEndpoint {

  private final SortedSet<String> resources;

  public ResourcesEndpoint(SortedSet<String> resources) {
    this.resources = resources;
  }

  @Override public Object get() {
    return resources;
  }

  @Override public Object get(String path) {
    return null;
  }
}
