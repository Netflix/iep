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

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

import com.google.common.io.Resources;
import com.google.common.base.Charsets;

import com.netflix.archaius.api.Config;
import com.netflix.config.ConfigurationManager;
import com.netflix.archaius.config.MapConfig;

public class TestResourceConfiguration {
  private TestResourceConfiguration() {}

  public static Config load(String propFile) throws Exception {
    return load(propFile, new HashMap<>());
  }

  public static Config load(String propFile, Map<String,String> subs) throws Exception {
    return load(propFile, subs, new HashMap<>());
  }

  public static Config load(
      String propFile, Map<String,String> subs, Map<String,String> overrides) throws Exception {
    URL propUrl = Resources.getResource(propFile);
    String propData = Resources.toString(propUrl, Charsets.UTF_8);
    for (Map.Entry e : subs.entrySet()) {
      propData = propData.replaceAll("\\{" + e.getKey() + "\\}", (String) e.getValue());
    }
    final Properties props = new Properties();
    props.load(new ByteArrayInputStream(propData.getBytes()));
    for (Map.Entry e : overrides.entrySet()) {
      props.setProperty((String) e.getKey(), (String) e.getValue());
    }

    for (String k : props.stringPropertyNames()) {
      ConfigurationManager.getConfigInstance().setProperty(k, props.getProperty(k));
    }

    return new MapConfig(props);
  }
}
