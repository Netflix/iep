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

import com.netflix.karyon.admin.rest.AdminHttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;


@Singleton
class DefaultHandler implements HttpHandler {

  private final AdminConfig config;
  private final AdminHttpHandler karyon;

  @Inject
  DefaultHandler(AdminConfig config, AdminHttpHandler karyon) {
    this.config = config;
    this.karyon = karyon;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    String path = exchange.getRequestURI().getPath();
    if (path == null
        || path.equals("/")
        || path.startsWith("/baseserver")
        || path.startsWith("/admin")
        || path.startsWith("/ui")) {
      exchange.getResponseHeaders().add("Location", config.uiLocation());
      exchange.sendResponseHeaders(302, -1);
    } else {
      karyon.handle(exchange);
    }
  }
}
