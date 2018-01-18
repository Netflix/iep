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
package com.netflix.iep.userservice;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.multibindings.Multibinder;
import com.netflix.iep.admin.guice.AdminModule;
import com.netflix.iep.service.Service;
import com.netflix.spectator.api.NoopRegistry;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.sandbox.HttpClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Setup bindings for {@link UserService}.
 */
public class UserServiceModule extends AbstractModule {
  @Override protected void configure() {
    bind(Context.class).toProvider(ContextProvider.class);
    bind(UserService.class).to(CompositeUserService.class);

    // Set used for composite
    Multibinder<UserService> userBinder = Multibinder.newSetBinder(binder(), UserService.class);
    userBinder.addBinding().to(DepartedUserService.class);
    userBinder.addBinding().to(EmployeeUserService.class);
    userBinder.addBinding().to(SimpleUserService.class);
    userBinder.addBinding().to(WhitelistUserService.class);

    // Show individual startup as part of servicemanager for improved debugging
    Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);
    serviceBinder.addBinding().to(DepartedUserService.class);
    serviceBinder.addBinding().to(EmployeeUserService.class);
    serviceBinder.addBinding().to(SimpleUserService.class);
    serviceBinder.addBinding().to(WhitelistUserService.class);
    serviceBinder.addBinding().to(CompositeUserService.class);

    // Register endpoint to admin to aid in debugging
    AdminModule.endpointsBinder(binder()).addBinding("/userservice").to(UserServiceEndpoint.class);
  }

  private static Config defaultConfig() {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) {
      cl = UserServiceModule.class.getClassLoader();
    }
    return ConfigFactory.load(cl);
  }

  @Singleton
  private static class ContextProvider implements Provider<Context> {

    @Inject(optional = true)
    private Registry registry = new NoopRegistry();

    @Inject(optional = true)
    private Config config = defaultConfig();

    @Inject(optional = true)
    private HttpClient client = HttpClient.DEFAULT;

    @Override public Context get() {
      return new Context(registry, config, client);
    }
  }
}
