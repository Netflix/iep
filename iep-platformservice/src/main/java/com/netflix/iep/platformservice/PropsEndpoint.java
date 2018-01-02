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
package com.netflix.iep.platformservice;

import com.netflix.archaius.api.Config;
import com.netflix.iep.admin.HttpEndpoint;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Summarizes properties in the archaius2 config similar to the default /env or /system
 * endpoints.
 */
public class PropsEndpoint implements HttpEndpoint {

  private final Config config;

  @Inject
  public PropsEndpoint(Config config) {
    this.config = config;
  }

  @Override public Object get() {
    return toMap(config.getKeys());
  }

  @Override public Object get(String path) {
    return toMap(config.getKeys(path));
  }

  private Map<String, String> toMap(Iterator<String> keys) {
    Map<String, String> props = new TreeMap<>();
    while (keys.hasNext()) {
      String k = keys.next();
      String v = config.getString(k, null);
      if (v != null) {
        props.put(k, v);
      }
    }
    return props;
  }
}
