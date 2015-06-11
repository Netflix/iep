/*
 * Copyright 2015 Netflix, Inc.
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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.netflix.config.ConfigurationManager;
import com.netflix.discovery.DiscoveryClient;
import org.apache.commons.configuration.Configuration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URI;

@RunWith(JUnit4.class)
public class EurekaModuleTest {

  private static final Module archaius = new AbstractModule() {
    @Override
    protected void configure() {
      bind(Configuration.class).toInstance(ConfigurationManager.getConfigInstance());
    }
  };

  @Test
  public void getClient() {
    Injector injector = Guice.createInjector(archaius, new EurekaModule());
    DiscoveryClient client = injector.getInstance(DiscoveryClient.class);
    Assert.assertNotNull(client);
  }

}
