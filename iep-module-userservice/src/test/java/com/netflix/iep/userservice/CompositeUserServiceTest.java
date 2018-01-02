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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@RunWith(JUnit4.class)
public class CompositeUserServiceTest {

  @Test
  public void emptySet() {
    UserService service = new CompositeUserService(Collections.emptySet());
    Assert.assertEquals(0, service.emailAddresses().size());
    Assert.assertFalse(service.isValidEmail("foo@example.com"));
    Assert.assertNull(service.toValidEmail("foo@example.com"));
  }

  @Test
  public void singletonSet() {
    UserService service = new CompositeUserService(
        Collections.singleton(() -> Collections.singleton("foo@example.com")));
    Assert.assertEquals(1, service.emailAddresses().size());
    Assert.assertTrue(service.isValidEmail("foo@example.com"));
    Assert.assertFalse(service.isValidEmail("bar@example.com"));
    Assert.assertEquals("foo@example.com", service.toValidEmail("foo@example.com"));
  }

  @Test
  public void multiSet() {
    Set<UserService> services = new HashSet<>();
    services.add(() -> Collections.singleton("foo@example.com"));
    services.add(() -> Collections.singleton("bar@example.com"));
    services.add(() -> Collections.singleton("baz@example.com"));
    UserService service = new CompositeUserService(services);
    Assert.assertEquals(3, service.emailAddresses().size());
    Assert.assertTrue(service.isValidEmail("foo@example.com"));
    Assert.assertTrue(service.isValidEmail("bar@example.com"));
    Assert.assertFalse(service.isValidEmail("abc@example.com"));
    Assert.assertEquals("foo@example.com", service.toValidEmail("foo@example.com"));
    Assert.assertEquals("bar@example.com", service.toValidEmail("bar@example.com"));
    Assert.assertNull(service.toValidEmail("abc@example.com"));
  }
}
