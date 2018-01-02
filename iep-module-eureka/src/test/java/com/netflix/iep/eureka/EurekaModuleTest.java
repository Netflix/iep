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
package com.netflix.iep.eureka;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RunWith(JUnit4.class)
public class EurekaModuleTest {

  private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

  @Before
  public void before() {
    executor.scheduleWithFixedDelay(EurekaModuleTest::dumpStack, 5, 30, TimeUnit.SECONDS);
  }

  @After
  public void after() {
    executor.shutdownNow();
  }

  @Test
  public void getClient() {
    Injector injector = Guice.createInjector(new EurekaModule());
    DiscoveryClient client = injector.getInstance(DiscoveryClient.class);
    Assert.assertNotNull(client);
  }

  @Test
  public void getEurekaClient() throws Exception {
    Injector injector = Guice.createInjector(new EurekaModule());
    EurekaClient client = injector.getInstance(EurekaClient.class);
    Assert.assertNotNull(client);
  }

  private static void dumpStack() {
    for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
      Thread t = entry.getKey();
      StackTraceElement[] trace = entry.getValue();
      System.out.println(t.getName());
      for (StackTraceElement element : trace) {
        System.out.printf("  at %s%n", element.toString());
      }
    }
  }

}
