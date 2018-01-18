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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.CloudInstanceConfig;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.config.ConfigurationManager;
import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.transport.jersey.Jersey1DiscoveryClientOptionalArgs;
import com.netflix.iep.admin.AdminServer;
import com.netflix.iep.admin.guice.AdminModule;
import com.netflix.iep.service.Service;
import org.apache.commons.configuration.Configuration;

import javax.inject.Named;
import javax.inject.Singleton;


/**
 * Setup eureka and create binding for DiscoveryClient.
 */
public final class EurekaModule extends AbstractModule {

  private static class OptionalInjections {
    @Inject(optional = true)
    @Named("IEP")
    private Configuration config;

    Configuration getConfig() {
      return (config == null) ? ConfigurationManager.getConfigInstance() : config;
    }
  }

  @Override protected void configure() {
    // InstanceInfo
    bind(InstanceInfo.class).toProvider(InstanceInfoProvider.class);

    // Needs:
    // * EurekaInstanceConfig
    // * InstanceInfo
    bind(ApplicationInfoManager.class).asEagerSingleton();

    // EurekaClientConfig
    bind(EurekaClientConfig.class).to(DefaultEurekaClientConfig.class).asEagerSingleton();

    // DiscoveryClientOptionalArgs, as of 1.6.0 we need to have an explicit binding. Using
    // Jersey1 as it is recommended by runtime team.
    bind(AbstractDiscoveryClientOptionalArgs.class)
        .to(Jersey1DiscoveryClientOptionalArgs.class).asEagerSingleton();

    // BackupRegistry, automatic via ImplementedBy annotation

    // Needs:
    // * InstanceInfo
    // * EurekaClientConfig
    // * DiscoveryClientOptionalArgs
    // * BackupRegistry
    bind(DiscoveryClient.class).asEagerSingleton();
    bind(HealthCheckHandler.class).toProvider(HandlerProvider.class).asEagerSingleton();

    Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);
    serviceBinder.addBinding().to(EurekaService.class);

    // Register endpoint to admin to aid in debugging
    AdminModule.endpointsBinder(binder()).addBinding("/eureka").to(EurekaEndpoint.class);
  }

  // Ensure that archaius configuration is setup prior to creating the eureka classes
  @Provides
  @Singleton
  private EurekaInstanceConfig provideInstanceConfig(OptionalInjections opts) {
    return new CloudInstanceConfig("netflix.appinfo.");
  }

  @Override public boolean equals(Object obj) {
    return obj != null && getClass().equals(obj.getClass());
  }

  @Override public int hashCode() {
    return getClass().hashCode();
  }

  public static void main(String[] args) {
    System.setProperty("netflix.iep.archaius.use-dynamic", "false");
    Injector injector = Guice.createInjector(new EurekaModule());
    AdminServer server = injector.getInstance(AdminServer.class);
    server.start();
  }
}
