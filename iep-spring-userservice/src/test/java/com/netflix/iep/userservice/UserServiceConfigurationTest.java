/*
 * Copyright 2014-2022 Netflix, Inc.
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

import com.netflix.iep.service.Service;
import com.netflix.iep.service.ServiceManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@RunWith(JUnit4.class)
public class UserServiceConfigurationTest {

  @Test
  public void services() throws Exception {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(UserServiceConfiguration.class);
      context.register(ServiceConfiguration.class);
      context.refresh();
      context.start();

      ServiceManager manager = context.getBean(ServiceManager.class);
      Assert.assertNotNull(manager);

      Set<String> expected = new TreeSet<>();
      expected.add("WhitelistUserService");
      expected.add("HttpUserService");
      expected.add("CompositeUserService");
      Set<String> result = manager.services().stream()
          .map(Service::name)
          .collect(Collectors.toSet());
      Assert.assertEquals(expected, result);
    }
  }

  @Configuration
  public static class ServiceConfiguration {

    @Bean
    ServiceManager serviceManager(Set<Service> services) {
      return new ServiceManager(services);
    }
  }

  @Test
  public void userService() throws Exception {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(UserServiceConfiguration.class);
      context.refresh();
      context.start();
      UserService s = context.getBean(UserService.class);
      Assert.assertNotNull(s);
      Assert.assertTrue(s instanceof CompositeUserService);
    }
  }

  @Test
  public void specificUserService() throws Exception {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(UserServiceConfiguration.class);
      context.refresh();
      context.start();
      WhitelistUserService s = context.getBean(WhitelistUserService.class);
      Assert.assertNotNull(s);
    }
  }
}
