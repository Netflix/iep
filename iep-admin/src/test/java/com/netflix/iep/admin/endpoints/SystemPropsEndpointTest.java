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
package com.netflix.iep.admin.endpoints;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Map;


@RunWith(JUnit4.class)
public class SystemPropsEndpointTest {

  private final SystemPropsEndpoint endpoint = new SystemPropsEndpoint();

  @Test @SuppressWarnings("unchecked")
  public void get() {
    Map<String, String> map = (Map<String, String>) endpoint.get();
    Assert.assertEquals(System.getProperties().size(), map.size());
    Assert.assertEquals(System.getProperty("java.version"), map.get("java.version"));
  }

  @Test @SuppressWarnings("unchecked")
  public void getProperty() {
    // jdk10+ have a java.version.date property
    Map<String, String> map = (Map<String, String>) endpoint.get("java.version");
    Assert.assertEquals(System.getProperty("java.version"), map.get("java.version"));
  }

  @Test @SuppressWarnings("unchecked")
  public void getPropertiesWithPrefix() {
    Map<String, String> map = (Map<String, String>) endpoint.get("java.");
    int size = (int) System.getProperties()
        .keySet()
        .stream()
        .filter(s -> s.toString().startsWith("java."))
        .count();
    Assert.assertEquals(size, map.size());
    Assert.assertEquals(System.getProperty("java.version"), map.get("java.version"));
  }
}
