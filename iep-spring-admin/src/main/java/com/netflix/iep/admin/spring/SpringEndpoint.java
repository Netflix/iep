/*
 * Copyright 2014-2022 Netflix, Inc.
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

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Endpoint that provides a list of keys available via the Spring ApplicationContext.
 */
@Singleton
public class SpringEndpoint implements HttpEndpoint {

  private final ApplicationContext context;

  @Inject
  public SpringEndpoint(ApplicationContext context) {
    this.context = context;
  }

  @Override public Object get() {
    return getMappings();
  }

  @Override public Object get(String path) {
    Pattern p = Pattern.compile(path);
    return getMappings()
        .entrySet()
        .stream()
        .filter(e -> p.matcher(e.getKey()).find() || p.matcher(e.getValue()).find())
        .collect(Collectors.toMap(
            Map.Entry<String, String>::getKey, Map.Entry<String, String>::getValue
        ));
  }

  private Map<String, String> getMappings() {
    Map<String, String> mappings = new HashMap<>();
    String[] names = context.getBeanDefinitionNames();
    for (String name : names) {
      mappings.put(name, context.getType(name).getName());
    }
    return mappings;
  }
}
