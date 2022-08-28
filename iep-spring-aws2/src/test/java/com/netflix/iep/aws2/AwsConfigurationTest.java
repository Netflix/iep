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
package com.netflix.iep.aws2;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.ec2.Ec2Client;

@RunWith(JUnit4.class)
public class AwsConfigurationTest {

  @Test
  public void createClient() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(AwsConfiguration.class);
      context.refresh();
      context.start();
      AwsClientFactory factory = context.getBean(AwsClientFactory.class);
      Ec2Client client = factory.newInstance(Ec2Client.class);
      Assert.assertNotNull(client);
    }
  }

  @Test
  public void createClientUsingProvider() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(AwsConfiguration.class, ClientConfiguration.class);
      context.refresh();
      context.start();
      Ec2Client client = context.getBean(Ec2Client.class);
      Assert.assertNotNull(client);
    }
  }

  @Configuration
  public static class ClientConfiguration {

    @Bean
    Ec2Client ec2Client(AwsClientFactory factory) {
      return factory.newInstance(Ec2Client.class);
    }
  }
}
