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
package com.netflix.iep.aws2;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import software.amazon.awssdk.services.ec2.EC2Client;

@RunWith(JUnit4.class)
public class AwsModuleTest {

  @Test
  public void createClient() {
    Injector injector = Guice.createInjector(new AwsModule());
    AwsClientFactory factory = injector.getInstance(AwsClientFactory.class);
    EC2Client client = factory.newInstance(EC2Client.class);
    Assert.assertNotNull(client);
  }

  @Test
  public void createClientUsingProvider() {
    Module module = new AbstractModule() {
      @Override protected void configure() {
      }

      @Provides
      private EC2Client providesEC2(AwsClientFactory factory) {
        return factory.newInstance(EC2Client.class);
      }
    };
    Injector injector = Guice.createInjector(module, new AwsModule());
    EC2Client client = injector.getInstance(EC2Client.class);
    Assert.assertNotNull(client);
  }
}
