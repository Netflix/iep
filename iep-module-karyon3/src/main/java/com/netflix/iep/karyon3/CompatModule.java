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
package com.netflix.iep.karyon3;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.karyon.admin.AdminServer;
import com.netflix.karyon.admin.HttpServerConfig;
import com.netflix.karyon.admin.SimpleHttpServer;
import com.netflix.karyon.admin.rest.AdminHttpHandler;
import com.sun.net.httpserver.HttpHandler;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;


public class CompatModule extends AbstractModule {

  @Provides
  @Singleton
  @AdminServer
  protected SimpleHttpServer getAdminServer(
      @AdminServer HttpServerConfig config,
      AdminHttpHandler handler,
      AppinfoHandler appinfoHandler,
      EnvHandler envHandler,
      JarHandler jarHandler,
      PropsHandler propsHandler) throws Exception {

    Map<String, HttpHandler> handlers = new HashMap<>();
    handlers.put("/", handler);
    handlers.put("/v1/platform/base/appinfo", appinfoHandler);
    handlers.put("/v1/platform/base/env",     envHandler);
    handlers.put("/v1/platform/base/jars",    jarHandler);
    handlers.put("/v1/platform/base/props",   propsHandler);

    return new SimpleHttpServer(config, handlers);
  }

  @Override
  protected void configure() {
  }
}
