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

import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SpringEnvEndpointTest {

  private AnnotationConfigApplicationContext createContext() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    Map<String, Object> p1 = new HashMap<>();
    p1.put("foo.bar", "1");
    Map<String, Object> p2 = new HashMap<>();
    p2.put("foo.bar", "2");
    p2.put("foo.baz", "abc");
    MutablePropertySources sources = context.getEnvironment().getPropertySources();
    sources.addFirst(new MapPropertySource("test2", p2));
    sources.addFirst(new MapPropertySource("test1", p1));
    context.refresh();
    return context;
  }

  private final SpringEnvEndpoint endpoint = new SpringEnvEndpoint(createContext());

  private int size(Object obj) {
    return (obj instanceof Map<?, ?>) ? ((Map<?, ?>) obj).size() : -1;
  }

  @SuppressWarnings("unchecked")
  private List<String> sourceNames(Object obj) {
    return (obj instanceof Map<?, ?>)
        ? new ArrayList<>(((Map<String, Object>) obj).keySet())
        : Collections.emptyList();
  }

  @Test
  public void get() {
    Assert.assertEquals(4, size(endpoint.get()));
    List<String> expected = new ArrayList<>();
    expected.add("test1");
    expected.add("test2");
    expected.add("systemProperties");
    expected.add("systemEnvironment");
    Assert.assertEquals(expected, sourceNames(endpoint.get()));
  }

  private Map<String, Object> propsMap(String... data) {
    Map<String, Object> props = new TreeMap<>();
    for (int i = 0; i < data.length; i += 2) {
      props.put(data[i], data[i + 1]);
    }
    return props;
  }

  @Test
  public void getPath() {
    Map<String, Map<String, Object>> expected = new LinkedHashMap<>();
    expected.put("test1", propsMap("foo.bar", "1"));
    expected.put("test2", propsMap("foo.bar", "2", "foo.baz", "abc"));
    Assert.assertEquals(expected, endpoint.get("foo"));
  }
}
