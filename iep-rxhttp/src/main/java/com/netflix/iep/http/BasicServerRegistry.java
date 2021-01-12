/*
 * Copyright 2014-2021 Netflix, Inc.
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
package com.netflix.iep.http;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry with a fixed set of servers specified at creation.
 *
 * @deprecated This library depends on RxNetty which is unsupported and RxJava 1.x which reached
 * end of life on March 31, 2018. Move to a supported HTTP client.
 */
@Deprecated
public class BasicServerRegistry implements ServerRegistry {

  private final Map<String, List<Server>> serversByVip;
  private final Set<Server> allServers;

  /** Create a new instance with no servers. */
  public BasicServerRegistry() {
    this(Collections.emptyMap());
  }

  /** Create a new instance. */
  public BasicServerRegistry(Map<String, List<Server>> serversByVip) {
    this.serversByVip = serversByVip;
    allServers = new HashSet<>();
    serversByVip.values().forEach(allServers::addAll);
  }

  @Override
  public boolean isStillAvailable(Server server) {
    return allServers.contains(server);
  }

  @Override
  public List<Server> getServers(String vip, ClientConfig clientCfg) {
    List<Server> results = serversByVip.get(vip);
    return (results != null) ? results : Collections.emptyList();
  }
}
