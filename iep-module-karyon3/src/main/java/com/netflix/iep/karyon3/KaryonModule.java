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
package com.netflix.iep.karyon3;


import com.google.inject.Provides;
import com.google.inject.grapher.NameFactory;
import com.google.inject.grapher.ShortNameFactory;
import com.google.inject.grapher.graphviz.PortIdFactory;
import com.google.inject.grapher.graphviz.PortIdFactoryImpl;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.util.Modules;
import com.netflix.archaius.ConfigProxyFactory;
import com.netflix.iep.service.Service;
import com.netflix.karyon.admin.AbstractAdminModule;
import com.netflix.karyon.admin.DIGraphResource;
import com.netflix.karyon.admin.JarsAdminResource;
import com.netflix.karyon.admin.ThreadsAdminResource;
import com.netflix.karyon.admin.rest.AdminServerModule;
import com.netflix.karyon.admin.ui.AdminUIServerConfig;
import com.netflix.karyon.archaius.admin.ArchaiusAdminModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;


public final class KaryonModule extends AbstractAdminModule {

  private static final Logger LOGGER = LoggerFactory.getLogger(KaryonModule.class);

  @Override protected void configure() {
    install(Modules.override(new AdminServerModule()).with(new CompatModule()));
    install(new ArchaiusAdminModule());

    // Resources from karyon3-core. Not using module because I don't necessarily want all
    // the core resources. In particular I'm not using the karyon healthcheck stuff.
    bindAdminResource("jars").to(JarsAdminResource.class);
    bindAdminResource("threads").to(ThreadsAdminResource.class);

    // These are needed in DIGraphResource
    bind(NameFactory.class).to(ShortNameFactory.class);
    bind(PortIdFactory.class).to(PortIdFactoryImpl.class);
    bindAdminResource("di-graph").to(DIGraphResource.class);

    Multibinder<Service> serviceBinder =
        Multibinder.newSetBinder(binder(), Service.class);
    bindAdminResource("services").to(ServicesResource.class);
  }

  // HACK to get around: https://github.com/Netflix/karyon/issues/310
  @Provides
  @Singleton
  protected AdminUIServerConfig getAdminServerConfig(ConfigProxyFactory factory) {
    return factory.newProxy(AdminUIServerConfig.class);
  }

  @Override public boolean equals(Object obj) {
    return obj != null && getClass().equals(obj.getClass());
  }

  @Override public int hashCode() {
    return getClass().hashCode();
  }
}
