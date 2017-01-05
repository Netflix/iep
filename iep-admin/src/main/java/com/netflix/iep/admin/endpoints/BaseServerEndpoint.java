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
package com.netflix.iep.admin.endpoints;

import com.netflix.iep.admin.HttpEndpoint;

import java.util.Map;
import java.util.TreeMap;

/**
 * Provides minimal backwards compatibility for endpoints from internal platform base-server.
 */
public class BaseServerEndpoint implements HttpEndpoint {

  private final JarsEndpoint jars = new JarsEndpoint();

  @Override public Object get() {
    return null;
  }

  @Override public Object get(String path) {
    Object obj;
    switch (path) {
      case "appinfo":  obj = getAppInfo(); break;
      case "env":      obj = getEnv();     break;
      case "jars":     obj = getJars();    break;
      default:         obj = null;         break;
    }
    return obj;
  }

  private Object getAppInfo() {
    Map<String, String> appinfo = new TreeMap<>();
    add(appinfo, "appName",  "NETFLIX_APP");
    add(appinfo, "hostID",   "EC2_INSTANCE_ID");
    add(appinfo, "hostName", "EC2_PUBLIC_HOSTNAME");
    add(appinfo, "ip",       "EC2_PUBLIC_IP");
    appinfo.put("javaVersion", System.getProperty("java.version"));
    Map<String, Object> wrapper = new TreeMap<>();
    wrapper.put("appinfo", new TreeMap<>(appinfo));
    return wrapper;
  }

  private void add(Map<String, String> info, String k, String p) {
    String v = System.getenv(p);
    if (v != null) {
      info.put(k, v);
    }
  }

  private Object getEnv() {
    Map<String, Object> wrapper = new TreeMap<>();
    wrapper.put("env", new TreeMap<>(System.getenv()));
    return wrapper;
  }

  private Object getJars() {
    Map<String, Object> wrapper = new TreeMap<>();
    wrapper.put("jars", jars.jars());
    return wrapper;
  }
}
