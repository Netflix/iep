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
import com.google.inject.multibindings.Multibinder;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.CloudInstanceConfig;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.DiscoveryManager;
import com.netflix.iep.service.Service;


/**
 * Setup eureka and create binding for DiscoveryClient.
 */
public class EurekaModule extends AbstractModule {

  @Override protected void configure() {
    final DiscoveryManager mgr = DiscoveryManager.getInstance();
    CloudInstanceConfig instanceCfg = new CloudInstanceConfig("netflix.appinfo.");
    DefaultEurekaClientConfig clientCfg = new DefaultEurekaClientConfig();
    mgr.initComponent(instanceCfg, clientCfg);
    bind(ApplicationInfoManager.class).toInstance(ApplicationInfoManager.getInstance());
    bind(DiscoveryClient.class).toInstance(mgr.getDiscoveryClient());
    bind(HealthCheckHandler.class).toProvider(HandlerProvider.class).asEagerSingleton();

    Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);
    serviceBinder.addBinding().to(EurekaService.class);
  }
}
