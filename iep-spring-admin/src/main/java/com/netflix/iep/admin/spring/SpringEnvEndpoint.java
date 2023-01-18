/*
 * Copyright 2014-2023 Netflix, Inc.
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
package com.netflix.iep.admin.spring;

import com.netflix.iep.admin.HttpEndpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Endpoint that provides an overview of properties in the Spring environment.
 */
public class SpringEnvEndpoint implements HttpEndpoint {

  private final ConfigurableEnvironment env;

  public SpringEnvEndpoint(ApplicationContext context) {
    this.env = (context instanceof GenericApplicationContext)
        ? ((GenericApplicationContext) context).getEnvironment()
        : null;
  }

  @Override public Object get() {
    return getPropertyMap();
  }

  @Override public Object get(String path) {
    Map<String, Map<String, Object>> sources = new LinkedHashMap<>();
    getPropertyMap().forEach((key, value) -> {
      Map<String, Object> filtered = filter(value, path);
      if (!filtered.isEmpty())
        sources.put(key, filtered);
    });
    return sources;
  }

  private Map<String, Object> filter(Map<String, Object> props, String prefix) {
    Map<String, Object> filtered = new TreeMap<>();
    props.entrySet()
        .stream()
        .filter(e -> e.getKey().startsWith(prefix))
        .forEach(e -> filtered.put(e.getKey(), e.getValue()));
    return filtered;
  }

  private Map<String, Map<String, Object>> getPropertyMap() {
    Map<String, Map<String, Object>> sources = new LinkedHashMap<>();
    if (env != null) {
      env.getPropertySources().stream().forEach(source -> {
        if (source instanceof EnumerablePropertySource<?>) {
          Map<String, Object> props = new TreeMap<>();
          String[] propNames = ((EnumerablePropertySource<?>) source).getPropertyNames();
          for (String propName : propNames) {
            Object v = source.getProperty(propName);
            if (v != null) {
              props.put(propName, v);
            }
          }
          sources.put(source.getName(), props);
        }
      });
    }
    return sources;
  }
}
