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
package com.netflix.iep.admin;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Handle paths without a more specific context.
 */
class DefaultHandler implements HttpHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultHandler.class);

  private final AdminConfig config;

  DefaultHandler(AdminConfig config) {
    this.config = config;
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
      exchange.sendResponseHeaders(404, -1);
    }
  }
}
