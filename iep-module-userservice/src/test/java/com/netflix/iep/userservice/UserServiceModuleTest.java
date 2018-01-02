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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.iep.service.Service;
import com.netflix.iep.service.ServiceManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@RunWith(JUnit4.class)
public class UserServiceModuleTest {

  @Test
  public void services() throws Exception {
    Injector injector = Guice.createInjector(new UserServiceModule());

    ServiceManager manager = injector.getInstance(ServiceManager.class);
    Assert.assertNotNull(manager);

    Set<String> expected = new TreeSet<>();
    expected.add("DepartedUserService");
    expected.add("EmployeeUserService");
    expected.add("SimpleUserService");
    expected.add("WhitelistUserService");
    expected.add("CompositeUserService");
    Set<String> result = manager.services().stream()
        .map(Service::name)
        .collect(Collectors.toSet());
    Assert.assertEquals(expected, result);
  }

  @Test
  public void userService() throws Exception {
    Injector injector = Guice.createInjector(new UserServiceModule());
    UserService s = injector.getInstance(UserService.class);
    Assert.assertNotNull(s);
    Assert.assertTrue(s instanceof CompositeUserService);
  }

  @Test
  public void specificUserService() throws Exception {
    Injector injector = Guice.createInjector(new UserServiceModule());
    SimpleUserService s = injector.getInstance(SimpleUserService.class);
    Assert.assertNotNull(s);
  }
}
