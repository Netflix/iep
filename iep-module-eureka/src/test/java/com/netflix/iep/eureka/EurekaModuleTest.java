/*
 * Copyright 2014-2016 Netflix, Inc.
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
package com.netflix.iep.eureka;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class EurekaModuleTest {

  @Test
  public void getClient() {
    Injector injector = Guice.createInjector(new EurekaModule());
    DiscoveryClient client = injector.getInstance(DiscoveryClient.class);
    Assert.assertNotNull(client);
  }

  @Test
  public void getEurekaClient() {
    Injector injector = Guice.createInjector(new EurekaModule());
    EurekaClient client = injector.getInstance(EurekaClient.class);
    Assert.assertNotNull(client);
  }

}
