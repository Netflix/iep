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
import com.netflix.archaius.config.MapConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;
import java.util.Map;


@RunWith(JUnit4.class)
public class PropsEndpointTest {

  private final Config config = new MapConfig(props());
  private final PropsEndpoint endpoint = new PropsEndpoint(config);

  private Map<String, String> props() {
    Map<String, String> map = new HashMap<>();
    map.put("java.version", "1.8.0");
    map.put("java.home",    "/apps/java");
    map.put("foo.bar.baz",  "abc");
    return map;
  }

  @Test @SuppressWarnings("unchecked")
  public void get() {
    Map<String, String> map = (Map<String, String>) endpoint.get();
    Assert.assertEquals(props().size(), map.size());
    Assert.assertEquals(config.getString("java.version"), map.get("java.version"));
  }

  @Test @SuppressWarnings("unchecked")
  public void getProperty() {
    Map<String, String> map = (Map<String, String>) endpoint.get("java.version");
    Assert.assertEquals(1, map.size());
    Assert.assertEquals(config.getString("java.version"), map.get("java.version"));
  }

  @Test @SuppressWarnings("unchecked")
  public void getPropertiesWithPrefix() {
    Map<String, String> map = (Map<String, String>) endpoint.get("java.");
    int size = (int) props()
        .keySet()
        .stream()
        .filter(s -> s.startsWith("java."))
        .count();
    Assert.assertEquals(size, map.size());
    Assert.assertEquals(config.getString("java.version"), map.get("java.version"));
  }
}
