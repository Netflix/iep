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

@RunWith(JUnit4.class)
public class WhitelistUserServiceTest {

  private ManualClock clock = new ManualClock();
  private Registry registry = new DefaultRegistry(clock);
  private Config config = ConfigFactory.load();
  private Context context = new Context(registry, config, HttpClient.DEFAULT);

  @Test
  public void start() throws Exception {
    WhitelistUserService service = new WhitelistUserService(context);
    Assert.assertEquals(2, service.emailAddresses().size());

    Assert.assertTrue(service.isValidEmail("foo@example.com"));
    Assert.assertTrue(service.isValidEmail("foo+alert@example.com"));
    Assert.assertTrue(service.isValidEmail("bar@example.com"));
    Assert.assertFalse(service.isValidEmail("baz@example.com"));

    Assert.assertEquals("foo@example.com", service.toValidEmail("foo@example.com"));
    Assert.assertEquals("foo+alert@example.com", service.toValidEmail("foo+alert@example.com"));
    Assert.assertEquals("foo@example.com", service.toValidEmail("abc@example.com"));
    Assert.assertEquals("foo@example.com", service.toValidEmail("abc+alert@example.com"));
    Assert.assertEquals("bar@example.com", service.toValidEmail("def@example.com"));
    Assert.assertNull(service.toValidEmail("ghi@example.com"));
  }
}
