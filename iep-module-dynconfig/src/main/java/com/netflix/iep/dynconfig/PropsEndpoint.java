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
package com.netflix.iep.dynconfig;

import com.netflix.iep.admin.HttpEndpoint;
import com.netflix.iep.config.DynamicConfigManager;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;

import javax.inject.Inject;
import java.util.Map;
import java.util.TreeMap;

/**
 * Summarizes properties in the Typesafe config similar to the default /env or /system
 * endpoints.
 */
public class PropsEndpoint implements HttpEndpoint {

  private final DynamicConfigManager manager;

  @Inject
  public PropsEndpoint(DynamicConfigManager manager) {
    this.manager = manager;
  }

  @Override public Object get() {
    return toMap(manager.get());
  }

  @Override public Object get(String path) {
    return toMap(manager.get().getConfig(path));
  }

  private Map<String, String> toMap(Config config) {
    Map<String, String> props = new TreeMap<>();
    for (Map.Entry<String, ConfigValue> entry : config.entrySet()) {
      String k = entry.getKey();
      String v = config.getValue(k).unwrapped().toString();
      if (v != null) {
        props.put(k, v);
      }
    }
    return props;
  }
}
