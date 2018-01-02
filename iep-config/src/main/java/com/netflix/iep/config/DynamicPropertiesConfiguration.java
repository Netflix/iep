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
package com.netflix.iep.config;

import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.PropertyFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
public class DynamicPropertiesConfiguration implements AutoCloseable {

  private final IConfiguration instance;

  @Inject
  public DynamicPropertiesConfiguration(PropertyFactory factory) {
    instance = new DynamicPropertiesConfigurationInstance(factory);
    Configuration.setConfiguration(instance);
  }

  protected IConfiguration getInstance() {
    return instance;
  }

  protected class DynamicPropertiesConfigurationInstance implements IConfiguration {
    private final PropertyFactory factory;
    private final Map<String, Property<String>> props = new ConcurrentHashMap<>();

    protected DynamicPropertiesConfigurationInstance(PropertyFactory factory) {
      this.factory = factory;
    }

    public String get(String key) {
      Property<String> prop = props.get(key);
      if (prop == null) {
        prop = factory.getProperty(key).asString(null);
        props.put(key, prop);
      }
      return prop.get();
    }
  }

  /**
   * @deprecated Use {@link #close()} instead.
   */
  public void destroy() {
    try {
      close();
    } catch (Exception e) {
      throw new RuntimeException("failed to shutdown dynamic properties configuration", e);
    }
  }

  @Override public void close() throws Exception {
    Configuration.setConfiguration(null);
  }
}
