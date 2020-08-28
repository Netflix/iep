/*
 * Copyright 2014-2020 Netflix, Inc.
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
package com.netflix.iep.dynconfig;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.netflix.iep.admin.guice.AdminModule;
import com.netflix.iep.config.ConfigManager;
import com.netflix.iep.config.DynamicConfigManager;
import com.netflix.iep.service.Service;
import com.netflix.spectator.api.DefaultRegistry;
import com.netflix.spectator.api.Registry;

/**
 * Configures the {@link ConfigManager#dynamicConfigManager()} to update the override layer
 * with properties from a remote property service.
 */
public final class DynamicConfigModule extends AbstractModule {

  @Override protected void configure() {
    Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);
    serviceBinder.addBinding().to(DynamicConfigService.class);

    AdminModule.endpointsBinder(binder()).addBinding("/props").to(PropsEndpoint.class);
  }

  @Provides
  @Singleton
  DynamicConfigService providesDynamicConfigService(OptionalInjections opts) {
    return new DynamicConfigService(opts.getRegistry());
  }

  @Provides
  @Singleton
  DynamicConfigManager providesDynamicConfigManager() {
    return ConfigManager.dynamicConfigManager();
  }

  @Override public boolean equals(Object obj) {
    return obj != null && getClass().equals(obj.getClass());
  }

  @Override public int hashCode() {
    return getClass().hashCode();
  }

  private static class OptionalInjections {
    @Inject(optional = true)
    private Registry registry;

    Registry getRegistry() {
      if (registry == null) {
        registry = new DefaultRegistry();
      }
      return registry;
    }
  }
}
