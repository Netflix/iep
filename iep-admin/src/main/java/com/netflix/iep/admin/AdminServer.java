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

import com.netflix.iep.admin.endpoints.ResourcesEndpoint;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Simple side server used to provide debugging information for the main application.
 */
@Singleton
public class AdminServer implements AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(AdminServer.class);

  private final AdminConfig config;
  private final HttpServer server;

  @Inject
  public AdminServer(AdminConfig config, @AdminEndpoint Map<String, Object> endpoints)
      throws IOException {
    this.config = config;

    InetSocketAddress address = new InetSocketAddress(config.port());
    this.server = HttpServer.create(address, config.backlog());

    TreeSet<String> paths = new TreeSet<>(endpoints.keySet());
    for (String path : paths.descendingSet()) {
      Object obj = endpoints.get(path);
      HttpEndpoint endpoint = (obj instanceof HttpEndpoint)
          ? (HttpEndpoint) obj
          : new BasicHttpEndpoint(obj);
      createContext(path, new RequestHandler(path, endpoint));
    }

    SortedSet<String> resources = new TreeSet<>(paths.stream()
        .map(p -> p.substring(1))
        .collect(Collectors.toSet()));
    resources.add("resources");
    createContext("/resources",
        new RequestHandler("/resources", new ResourcesEndpoint(resources)));

    StaticResourceHandler staticHandler = new StaticResourceHandler(
        Thread.currentThread().getContextClassLoader(),
        Collections.singletonMap("/ui", "static/index.html"));
    createContext("/static", staticHandler);
    createContext("/ui", staticHandler);

    createContext("/", new DefaultHandler(config));

    server.start();
    LOGGER.info("started on port {}", config.port());
  }

  private void createContext(String path, HttpHandler handler) {
    server.createContext(path, new AccessLogHandler(handler));
  }

  /**
   * @deprecated This is a no-op, the server will be automatically started when it
   * is constructed.
   */
  @Deprecated
  public void start() {
  }

  /**
   * @deprecated Use {@link #close()} instead.
   */
  @Deprecated
  public void stop() {
    try {
      close();
    } catch (Exception e) {
      throw new RuntimeException("failed to stop AdminServer", e);
    }
  }

  @Override public void close() throws Exception {
    LOGGER.info("shutting down admin on port {}", config.port());
    server.stop((int) config.shutdownDelay().toMillis());
  }
}
