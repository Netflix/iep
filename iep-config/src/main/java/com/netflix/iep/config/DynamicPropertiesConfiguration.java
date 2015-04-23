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
package com.netflix.iep.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.netflix.config.DynamicStringProperty;

public class DynamicPropertiesConfiguration implements IConfiguration {
  private final String prefix;

  public DynamicPropertiesConfiguration() {
    this(null);
  }

  public DynamicPropertiesConfiguration(String prefix) {
    this.prefix = prefix;
  }

  private final Map<String, DynamicStringProperty> props =
    new ConcurrentHashMap<String, DynamicStringProperty>();

  public String get(String key) {
    String propKey = (prefix == null) ? key : prefix + "." + key;
    DynamicStringProperty prop = props.get(propKey);
    if (prop == null) {
      prop = new DynamicStringProperty(propKey, null);
      props.put(propKey, prop);
    }
    return prop.get();
  }
}
