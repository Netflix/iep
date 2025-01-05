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
package com.netflix.iep.servergroups;

import com.netflix.spectator.ipc.http.HttpClient;

import java.net.URI;
import java.util.function.Predicate;

final class LoaderUtils {

  private LoaderUtils() {
  }

  static final URI EDDA_URI = URI.create("http://localhost:7101/api/v2/netflix/serverGroups");

  static EddaLoader createEddaLoader(String resource, boolean gzip) throws Exception {
    HttpClient client = TestHttpClient.resource(200, resource, gzip);
    return new EddaLoader(client, EDDA_URI);
  }

  static final URI EUREKA_URI = URI.create("http://localhost:7101/v2/apps");

  static EurekaLoader createEurekaLoader(String resource, String account) throws Exception {
    HttpClient client = TestHttpClient.resource(200, resource, false);
    Predicate<String> p = (account == null) ? v -> true : account::equals;
    return new EurekaLoader(client, EUREKA_URI, p);
  }
}
