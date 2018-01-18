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
package com.netflix.iep.admin.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.netflix.iep.admin.AdminConfig;
import com.netflix.iep.admin.AdminEndpoint;
import com.netflix.iep.admin.AdminServer;
import com.netflix.iep.admin.endpoints.BaseServerEndpoint;
import com.netflix.iep.admin.endpoints.EnvEndpoint;
import com.netflix.iep.admin.endpoints.JarsEndpoint;
import com.netflix.iep.admin.endpoints.JmxEndpoint;
import com.netflix.iep.admin.endpoints.ReflectEndpoint;
import com.netflix.iep.admin.endpoints.ServicesEndpoint;
import com.netflix.iep.admin.endpoints.SpectatorEndpoint;
import com.netflix.iep.admin.endpoints.SystemPropsEndpoint;
import com.netflix.iep.admin.endpoints.ThreadsEndpoint;
import com.netflix.iep.service.Service;
import com.netflix.spectator.api.DefaultRegistry;
import com.netflix.spectator.api.Registry;

import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Map;

/**
 * Configure {@link AdminServer} via Guice. The server will be setup as an eager
 * singleton, but must be explicitly started if not using a lifecycle manager with
 * guice to automatically call the {@link javax.annotation.PostConstruct} method.
 */
public class AdminModule extends AbstractModule {

  /**
   * Helper for getting an instance of MapBinder to add a custom endpoint. Usage:
   *
   * <pre>
   * public class MyModule extends AbstractModule {
   *   @Override protected void configure() {
   *     AdminModule.endpointBinder(binder())
   *       .addBinding("/foo").to(FooEndpoint.class);
   *   }
   * }
   * </pre>
   */
  public static MapBinder<String, Object> endpointsBinder(Binder binder) {
    return MapBinder.newMapBinder(binder, String.class, Object.class, AdminEndpoint.class);
  }

  @Override protected void configure() {
    // Default set of endpoints
    MapBinder<String, Object> endpoints = endpointsBinder(binder());
    endpoints.addBinding("/env").to(EnvEndpoint.class);
    endpoints.addBinding("/system").to(SystemPropsEndpoint.class);
    endpoints.addBinding("/jars").to(JarsEndpoint.class);
    endpoints.addBinding("/jmx").to(JmxEndpoint.class);
    endpoints.addBinding("/threads").to(ThreadsEndpoint.class);
    endpoints.addBinding("/spectator").to(SpectatorEndpoint.class);
    endpoints.addBinding("/services").to(ServicesEndpoint.class);
    endpoints.addBinding("/v1/platform/base").to(BaseServerEndpoint.class);
    endpoints.addBinding("/debug").to(GuiceDebugEndpoint.class);
    endpoints.addBinding("/guice").to(GuiceEndpoint.class);

    // Init set of services to an empty set
    Multibinder.newSetBinder(binder(), Service.class);

    bind(AdminServer.class).toProvider(AdminServerProvider.class).asEagerSingleton();
  }

  private static class OptionalInjections {
    @Inject(optional = true)
    private Registry registry;

    @Inject(optional = true)
    private AdminConfig config;

    Registry getRegistry() {
      if (registry == null) {
        registry = new DefaultRegistry();
      }
      return registry;
    }

    AdminConfig getConfig() {
      if (config == null) {
        config = AdminConfig.DEFAULT;
      }
      return config;
    }
  }

  @Singleton
  private static class DebugProvider implements Provider<ReflectEndpoint> {

    private final ReflectEndpoint endpoint;

    @Inject
    DebugProvider(Injector injector) {
      endpoint = new ReflectEndpoint(injector);
    }

    @Override public ReflectEndpoint get() {
      return endpoint;
    }
  }

  @Singleton
  private static class RegistryProvider implements Provider<Registry> {

    private final Registry registry;

    @Inject
    RegistryProvider(OptionalInjections opts) {
      this.registry = opts.getRegistry();
    }

    @Override public Registry get() {
      return registry;
    }
  }

  @Singleton
  private static class AdminServerProvider implements Provider<AdminServer> {

    private final AdminServer server;

    @Inject
    AdminServerProvider(OptionalInjections opts, @AdminEndpoint Map<String, Object> endpoints)
        throws IOException {
      this.server = new AdminServer(opts.getConfig(), endpoints);
    }

    @Override public AdminServer get() {
      return server;
    }
  }

  /**
   * Sample main that runs the admin with a default set of endpoints. Mostly used for
   * quick local testing of the module and common endpoints.
   */
  public static void main(String[] args) {
    Injector injector = Guice.createInjector(new AdminModule(), new AbstractModule() {
      @Override protected void configure() {
        bind(Registry.class).toInstance(new DefaultRegistry());
      }
    });
    AdminServer server = injector.getInstance(AdminServer.class);
    server.start();
  }
}
