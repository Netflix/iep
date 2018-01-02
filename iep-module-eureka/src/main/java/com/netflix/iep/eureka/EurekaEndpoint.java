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

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.iep.admin.HttpEndpoint;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Provide visibility into current state of instances for the eureka client. Supported
 * views are:
 *
 * <pre>
 * /status
 *     Return information the status information for the local instance.
 * /apps
 *     Returns a list of application names.
 * /apps/${app}
 *     Returns instance information for an application.
 * /vips
 *     Returns a list of vip names.
 * /vips/${vip}
 *     Returns instance information for a vip.
 * /instances
 *     Returns a list of all instances.
 * /instances/${instance}
 *     Returns information for a specific instance.
 * </pre>
 */
public class EurekaEndpoint implements HttpEndpoint {

  private static final String[] OPTIONS = new String[] {
      "status", "apps", "vips", "instances"
  };

  private final ApplicationInfoManager appinfo;
  private final EurekaClient client;

  @Inject
  public EurekaEndpoint(ApplicationInfoManager appinfo, EurekaClient client) {
    this.appinfo = appinfo;
    this.client = client;
  }

  @Override public Object get() {
    return OPTIONS;
  }

  @Override public Object get(String path) {
    Object obj;
    String[] parts = path.split("/");
    if (parts.length == 1) {
      switch (parts[0]) {
        case "status":     obj = getStatus();           break;
        case "apps":       obj = getApps();             break;
        case "vips":       obj = getVips();             break;
        case "instances":  obj = getInstances();        break;
        default:           obj = null;                  break;
      }
    } else if (parts.length == 2) {
      switch (parts[0]) {
        case "apps":       obj = getApp(parts[1]);      break;
        case "vips":       obj = getVip(parts[1]);      break;
        case "instances":  obj = getInstance(parts[1]); break;
        default:           obj = null;                  break;
      }
    } else {
      obj = null;
    }
    return obj;
  }

  private InstanceInfo getStatus() {
    return appinfo.getInfo();
  }

  private Set<String> getApps() {
    Set<String> apps = client.getApplications()
        .getRegisteredApplications()
        .stream()
        .map(a -> a.getName().toLowerCase())
        .collect(Collectors.toSet());
    return new TreeSet<>(apps);
  }

  private List<InstanceInfo> getApp(String app) {
    return client.getApplication(app.toUpperCase()).getInstances();
  }

  private Set<String> getVips() {
    Set<String> vips = client.getApplications()
        .getRegisteredApplications()
        .stream()
        .flatMap(app -> app.getInstances().stream().map(InstanceInfo::getVIPAddress))
        .filter(v -> v != null)
        .collect(Collectors.toSet());
    return new TreeSet<>(vips);
  }

  private List<InstanceInfo> getVip(String vip) {
    return client.getInstancesByVipAddress(vip, false);
  }

  private Set<String> getInstances() {
    Set<String> instances = client.getApplications()
        .getRegisteredApplications()
        .stream()
        .flatMap(app -> app.getInstances().stream().map(InstanceInfo::getInstanceId))
        .filter(v -> v != null)
        .collect(Collectors.toSet());
    return new TreeSet<>(instances);
  }

  @SuppressWarnings("unchecked")
  private List<InstanceInfo> getInstance(String id) {
    return client.getInstancesById(id);
  }
}
