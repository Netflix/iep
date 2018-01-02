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

import com.netflix.spectator.api.DefaultRegistry;
import com.netflix.spectator.api.ManualClock;
import com.netflix.spectator.api.Registry;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.TreeMap;
import java.util.regex.Pattern;


@RunWith(JUnit4.class)
public class BaseServerEndpointTest {

  private final ManualClock clock = new ManualClock();
  private final Registry registry = new DefaultRegistry(clock);
  private final BaseServerEndpoint endpoint = new BaseServerEndpoint(registry);

  @Test
  public void get() {
    Assert.assertNull(endpoint.get());
  }

  @Test
  public void getAppInfo() {
    String javaVersion = System.getProperty("java.version");
    Assert.assertTrue(endpoint.get("appinfo").toString().contains(javaVersion));
  }

  @Test
  public void getJars() {
    Pattern spectatorJar = Pattern.compile("spectator-api-[^\",]+\\.jar");
    String response = endpoint.get("jars").toString();
    Assert.assertTrue(spectatorJar.matcher(response).find());
  }

  @Test
  public void getEnv() {
    String response = endpoint.get("env").toString();
    String expected = new TreeMap<>(System.getenv()).toString();
    Assert.assertTrue(response.contains(expected));
  }

  @Test
  public void getMetrics() {
    String response = endpoint.get("metrics").toString();
    Assert.assertEquals(response, "[]");

    registry.gauge("test.gauge", "foo", "bar", "abc", "def").set(42.0);
    response = endpoint.get("metrics").toString();
    String expected = "[{name=test.gauge, tags={abc=def, foo=bar}, timestamp=0, value=42.0}]";
    Assert.assertEquals(response, expected);
  }
}
