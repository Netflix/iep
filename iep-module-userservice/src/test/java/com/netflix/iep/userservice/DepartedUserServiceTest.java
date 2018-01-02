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
import com.netflix.spectator.sandbox.HttpResponse;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

@RunWith(JUnit4.class)
public class DepartedUserServiceTest {

  private ManualClock clock = new ManualClock();
  private Registry registry = new DefaultRegistry(clock);
  private Config config = ConfigFactory.load();

  private Context newContext(HttpClient client) {
    return new Context(registry, config, client);
  }

  private HttpResponse ok(String data) {
    return new HttpResponse(200, Collections.emptyMap(), data.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  public void start() throws Exception {
    Context ctxt = newContext(new TestClient(() -> ok("{\"bar@example.com\":\"foo@example.com\"}")));
    DepartedUserService service = new DepartedUserService(ctxt);
    Assert.assertEquals(0, service.emailAddresses().size());

    service.start();
    Assert.assertEquals(1, service.emailAddresses().size());
    Assert.assertTrue(service.isValidEmail("foo@example.com"));
    Assert.assertFalse(service.isValidEmail("bar@example.com"));
    Assert.assertEquals("foo@example.com", service.toValidEmail("bar@example.com"));
    Assert.assertEquals("foo@example.com", service.toValidEmail("bar+alert@example.com"));

    service.stop();
  }


  @Test
  public void testCaseInsensitivity() throws Exception {
    Context ctxt = newContext(new TestClient(() -> ok("{\"email\":\"fooBar@example.com\"}")));
    DepartedUserService service = new DepartedUserService(ctxt);
    service.start();

    Set<String> emails = service.emailAddresses();
    Assert.assertEquals(1, emails.size());
    Assert.assertTrue(service.isValidEmail("foobar@example.com"));
    Assert.assertTrue(service.isValidEmail("FooBar@example.com"));
  }
}
