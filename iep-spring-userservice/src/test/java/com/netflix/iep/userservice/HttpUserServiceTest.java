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
package com.netflix.iep.userservice;

import com.netflix.spectator.api.DefaultRegistry;
import com.netflix.spectator.api.ManualClock;
import com.netflix.spectator.api.Registry;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class HttpUserServiceTest {

  private final ManualClock clock = new ManualClock();
  private final Registry registry = new DefaultRegistry(clock);
  private final Config config = ConfigFactory.load();
  Context ctxt = new Context(registry, config, new TestClient(null), null);

  @Test
  public void validEmail() {
    HttpUserService service = new HttpUserService(ctxt, "http-200");
    Assert.assertTrue(service.isValidEmail("bob@example.com"));
  }

  @Test
  public void unknownEmail() {
    HttpUserService service = new HttpUserService(ctxt, "http-404");
    Assert.assertFalse(service.isValidEmail("bob@example.com"));
  }

  @Test
  public void validOnFailure500() {
    HttpUserService service = new HttpUserService(ctxt, "http-500-ok");
    Assert.assertTrue(service.isValidEmail("bob@example.com"));
  }

  @Test
  public void notValidOnFailure500() {
    HttpUserService service = new HttpUserService(ctxt, "http-500-unknown");
    Assert.assertFalse(service.isValidEmail("bob@example.com"));
  }

  @Test
  public void validOnFailureException() {
    HttpUserService service = new HttpUserService(ctxt, "http-exception-ok");
    Assert.assertTrue(service.isValidEmail("bob@example.com"));
  }

  @Test
  public void notValidOnFailureException() {
    HttpUserService service = new HttpUserService(ctxt, "http-exception-unknown");
    Assert.assertFalse(service.isValidEmail("bob@example.com"));
  }

  @Test
  public void multipleLastValid() {
    HttpUserService service = new HttpUserService(ctxt, "http-multiple-200");
    Assert.assertTrue(service.isValidEmail("bob@example.com"));
  }

  @Test
  public void multipleAllUnknown() {
    HttpUserService service = new HttpUserService(ctxt, "http-multiple-404");
    Assert.assertFalse(service.isValidEmail("bob@example.com"));
  }

  @Test
  public void notEnabled() {
    HttpUserService service = new HttpUserService(ctxt, "http-not-enabled");
    Assert.assertFalse(service.isValidEmail("bob@example.com"));
  }

  @Test
  public void empty() {
    HttpUserService service = new HttpUserService(ctxt, "http-empty");
    Assert.assertFalse(service.isValidEmail("bob@example.com"));
  }
}
