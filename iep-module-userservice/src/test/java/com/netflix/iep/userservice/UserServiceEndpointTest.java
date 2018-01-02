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
package com.netflix.iep.userservice;

import com.netflix.spectator.api.DefaultRegistry;
import com.netflix.spectator.api.ManualClock;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.sandbox.HttpClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@RunWith(JUnit4.class)
public class UserServiceEndpointTest {

  private ManualClock clock = new ManualClock();
  private Registry registry = new DefaultRegistry(clock);
  private Config config = ConfigFactory.load();
  private Context context = new Context(registry, config, HttpClient.DEFAULT);

  @Test
  public void list() throws Exception {
    UserServiceEndpoint endpoint = new UserServiceEndpoint(new WhitelistUserService(context));

    Set<String> expected = new TreeSet<>();
    expected.add("bar@example.com");
    expected.add("foo@example.com");

    Assert.assertEquals(expected, endpoint.get());
  }

  @Test
  public void getAddrValid() throws Exception {
    UserServiceEndpoint endpoint = new UserServiceEndpoint(new WhitelistUserService(context));

    Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("isValidEmail", true);
    expected.put("toValidEmail", "foo+bar@example.com");

    Assert.assertEquals(expected, endpoint.get("foo+bar@example.com"));
  }

  @Test
  public void getAddrInvalidMapped() throws Exception {
    UserServiceEndpoint endpoint = new UserServiceEndpoint(new WhitelistUserService(context));

    Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("isValidEmail", false);
    expected.put("toValidEmail", "foo@example.com");

    Assert.assertEquals(expected, endpoint.get("abc@example.com"));
  }

  @Test
  public void getAddrInvalid() throws Exception {
    UserServiceEndpoint endpoint = new UserServiceEndpoint(new WhitelistUserService(context));

    Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("isValidEmail", false);
    expected.put("toValidEmail", null);

    Assert.assertEquals(expected, endpoint.get("ghi@example.com"));
  }
}
