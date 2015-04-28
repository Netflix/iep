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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.netflix.archaius.Property;
import com.netflix.archaius.PropertyFactory;

@Singleton
public class DynamicPropertiesConfiguration implements IConfiguration {

  private final PropertyFactory factory;

  @Inject
  public DynamicPropertiesConfiguration(PropertyFactory factory) {
    this.factory = factory;
  }

  private final Map<String, Property<String>> props = new ConcurrentHashMap<>();

  public String get(String key) {
    Property<String> prop = props.get(key);
    if (prop == null) {
      prop = factory.getProperty(key).asString(null);
      props.put(key, prop);
    }
    return prop.get();
  }

  @PostConstruct
  public void init() {
    Configuration.setConfiguration(this);
  }

  @PreDestroy
  public void destroy() {
    Configuration.setConfiguration(null);
  }
}
