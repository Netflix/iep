/*
 * Copyright 2014-2019 Netflix, Inc.
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
import software.amazon.awssdk.services.ec2.Ec2Client;

@RunWith(JUnit4.class)
public class AwsModuleTest {

  @Test
  public void createClient() {
    Injector injector = Guice.createInjector(new AwsModule());
    AwsClientFactory factory = injector.getInstance(AwsClientFactory.class);
    Ec2Client client = factory.newInstance(Ec2Client.class);
    Assert.assertNotNull(client);
  }

  @Test
  public void createClientUsingProvider() {
    Module module = new AbstractModule() {
      @Override protected void configure() {
      }

      @Provides
      private Ec2Client providesEC2(AwsClientFactory factory) {
        return factory.newInstance(Ec2Client.class);
      }
    };
    Injector injector = Guice.createInjector(module, new AwsModule());
    Ec2Client client = injector.getInstance(Ec2Client.class);
    Assert.assertNotNull(client);
  }
}
