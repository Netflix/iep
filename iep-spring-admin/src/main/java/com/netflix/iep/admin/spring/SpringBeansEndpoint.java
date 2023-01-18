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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Endpoint that provides a list of keys available via the Spring ApplicationContext.
 */
public class SpringBeansEndpoint implements HttpEndpoint {

  private final ApplicationContext context;

  public SpringBeansEndpoint(ApplicationContext context) {
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
        .filter(e -> p.matcher(e.getKey()).find() || matches(p, e.getValue()))
        .collect(Collectors.toMap(
            Map.Entry<String, Map<String, Object>>::getKey,
            Map.Entry<String, Map<String, Object>>::getValue,
            (a, b) -> a,
            TreeMap::new
        ));
  }

  private boolean matches(Pattern p, Map<String, Object> value) {
    return value.values().stream().anyMatch(obj -> {
      if (obj instanceof String) {
        return p.matcher((String) obj).find();
      } else if (obj instanceof String[]) {
        for (String s : (String[]) obj) {
          if (p.matcher(s).find())
            return true;
        }
        return false;
      } else {
        return false;
      }
    });
  }

  private ConfigurableListableBeanFactory getBeanFactory() {
    return (context instanceof GenericApplicationContext)
        ? ((GenericApplicationContext) context).getBeanFactory()
        : null;
  }

  private Map<String, Map<String, Object>> getMappings() {
    Map<String, Map<String, Object>> mappings = new TreeMap<>();
    String[] names = context.getBeanDefinitionNames();
    for (String name : names) {
      Map<String, Object> beanInfo = new TreeMap<>();
      beanInfo.put("class", context.getType(name).getName());
      ConfigurableListableBeanFactory factory = getBeanFactory();
      if (factory != null) {
        BeanDefinition def = factory.getBeanDefinition(name);
        beanInfo.put("aliases", factory.getAliases(name));
        beanInfo.put("resource", def.getResourceDescription());
        beanInfo.put("scope", def.getScope());
        beanInfo.put("singleton", factory.isSingleton(name));
        beanInfo.put("dependsOn", factory.getDependenciesForBean(name));
      }
      mappings.put(name, beanInfo);
    }
    return mappings;
  }
}
