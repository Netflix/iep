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
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Map;


@RunWith(JUnit4.class)
public class EnvEndpointTest {

  private final EnvEndpoint endpoint = new EnvEndpoint();

  @Test @SuppressWarnings("unchecked")
  public void get() {
    Assume.assumeTrue(System.getenv("JAVA_HOME") != null);
    Map<String, String> map = (Map<String, String>) endpoint.get();
    Assert.assertEquals(System.getenv().size(), map.size());
    Assert.assertEquals(System.getenv("JAVA_HOME"), map.get("JAVA_HOME"));
  }

  @Test @SuppressWarnings("unchecked")
  public void getProperty() {
    Assume.assumeTrue(System.getenv("JAVA_HOME") != null);
    Map<String, String> map = (Map<String, String>) endpoint.get("JAVA_HOME");
    Assert.assertEquals(1, map.size());
    Assert.assertEquals(System.getenv("JAVA_HOME"), map.get("JAVA_HOME"));
  }

  @Test @SuppressWarnings("unchecked")
  public void getPropertiesWithPrefix() {
    Assume.assumeTrue(System.getenv("JAVA_HOME") != null);
    Map<String, String> map = (Map<String, String>) endpoint.get("JAVA_");
    int size = (int) System.getenv()
        .keySet()
        .stream()
        .filter(s -> s.startsWith("JAVA_"))
        .count();
    Assert.assertEquals(size, map.size());
    Assert.assertEquals(System.getenv("JAVA_HOME"), map.get("JAVA_HOME"));
  }
}
