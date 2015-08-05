/*
 * Copyright 2015 Netflix, Inc.
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
package com.netflix.iep.karyon3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.archaius.Config;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;


@Singleton
class AppinfoHandler extends SimpleHandler {

  private final Map<String, String> appinfo;

  @Inject
  AppinfoHandler(ObjectMapper mapper, Config config) {
    super(mapper);

    Map<String, String> tmp = new TreeMap<>();
    add(tmp, config, "appName",     "netflix.appinfo.name");
    add(tmp, config, "port",        "netflix.appinfo.port");
    add(tmp, config, "vipAddr",     "netflix.appinfo.vipAddress");
    add(tmp, config, "hostID",      "EC2_INSTANCE_ID");
    add(tmp, config, "hostName",    "EC2_PUBLIC_HOSTNAME");
    add(tmp, config, "ip",          "EC2_PUBLIC_IP");
    add(tmp, config, "javaVersion", "java.version");
    appinfo = Collections.unmodifiableMap(tmp);
  }

  private void add(Map<String, String> info, Config config, String k, String p) {
    String v = config.getString(p, null);
    if (v != null) {
      info.put(k, v);
    }
  }

  @Override
  protected Object get() {
    // TreeMap is used for so user will conveniently get sorted keys, but is not required.
    Map<String, Object> wrapper = new TreeMap<>();
    wrapper.put("appinfo", appinfo);
    return wrapper;
  }
}
