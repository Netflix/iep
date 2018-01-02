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
package com.netflix.iep.admin;

/**
 * Represents a simple endpoint providing data for the admin.
 */
public interface HttpEndpoint {
  /**
   * Root call for the endpoint. This will typically list the set of resources that are
   * available. If null is returned, then the user will get a 404.
   */
  Object get();

  /**
   * Get a specific id. This will typically be a specific item or a filtered list of items
   * based on the query represented by the path. If null is returned, then the user will
   * get a 404.
   */
  Object get(String path);
}
