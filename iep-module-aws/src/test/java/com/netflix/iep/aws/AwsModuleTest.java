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
package com.netflix.iep.aws;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AwsModuleTest {

  @Test
  public void createClient() {
    Injector injector = Guice.createInjector(new AwsModule());
    AwsClientFactory factory = injector.getInstance(AwsClientFactory.class);
    AmazonEC2Client client = factory.newInstance(AmazonEC2Client.class);
    Assert.assertNotNull(client);
  }

  @Test
  public void createClientUsingProvider() {
    Module module = new AbstractModule() {
      @Override protected void configure() {
      }

      @Provides
      private AmazonEC2 providesEC2(AwsClientFactory factory) {
        return factory.newInstance(AmazonEC2.class);
      }
    };
    Injector injector = Guice.createInjector(module, new AwsModule());
    AmazonEC2 client = injector.getInstance(AmazonEC2.class);
    Assert.assertNotNull(client);
  }
}
