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
package com.netflix.iep.archaius2;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.iep.admin.AdminServer;
import com.netflix.iep.platformservice.PlatformServiceModule;

/**
 * Work around for overriding the AppConfig.
 */
public final class OverrideModule extends AbstractModule {
  @Override protected void configure() {
    Module m = Modules
        .override(new ArchaiusModule())
        .with(new PlatformServiceModule());
    install(m);
  }

  @Override public boolean equals(Object obj) {
    return obj != null && getClass().equals(obj.getClass());
  }

  @Override public int hashCode() {
    return getClass().hashCode();
  }

  public static void main(String[] args) {
    System.setProperty("netflix.iep.archaius.use-dynamic", "false");
    Injector injector = Guice.createInjector(new OverrideModule());
    AdminServer server = injector.getInstance(AdminServer.class);
    server.start();
  }
}
