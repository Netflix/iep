/*
 * Copyright 2014-2025 Netflix, Inc.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.ipc.http.HttpClient;
import com.netflix.spectator.ipc.http.HttpRequestBuilder;
import com.netflix.spectator.ipc.http.HttpResponse;
import com.typesafe.config.Config;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Common settings used across all services. */
public final class Context implements AutoCloseable {

  private final ObjectMapper mapper = new ObjectMapper();

  private final Registry registry;

  private final Config config;
  private final HttpClient client;

  private final SSLSocketFactory sslFactory;

  private final int connectTimeout;
  private final int readTimeout;

  private final int frequency;

  private final ScheduledExecutorService executor;

  public Context(Registry registry, Config config, HttpClient client, SSLSocketFactory sslFactory) {
    this.registry = registry;
    this.config = config.getConfig("netflix.iep.userservice");
    this.client = client;
    this.sslFactory = sslFactory;

    connectTimeout = (int) this.config.getDuration("connect-timeout", TimeUnit.MILLISECONDS);
    readTimeout = (int) this.config.getDuration("read-timeout", TimeUnit.MILLISECONDS);

    frequency = (int) this.config.getDuration("frequency", TimeUnit.MILLISECONDS);

    executor = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "iep-userservice");
      t.setDaemon(true);
      return t;
    });
  }

  /**
   * @deprecated Use {@link #close()} instead.
   */
  @Deprecated
  public void stop() {
    try {
      close();
    } catch (Exception e) {
      throw new RuntimeException("failed to stop user service Context", e);
    }
  }

  @Override public void close() throws Exception {
    executor.shutdown();
  }

  ObjectMapper objectMapper() {
    return mapper;
  }

  Registry registry() {
    return registry;
  }

  Config config() {
    return config;
  }

  HttpResponse get(String name, String uri) throws IOException {
    return get(name, uri, false);
  }

  HttpResponse get(String name, String uri, boolean useSslFactory) throws IOException {

    HttpRequestBuilder builder = client.get(URI.create(uri));
    if (useSslFactory) {
      builder.withSSLSocketFactory(sslFactory);
    }

    return builder
        .withConnectTimeout(connectTimeout)
        .withReadTimeout(readTimeout)
        .withRetries(2)
        .acceptJson()
        .acceptGzip()
        .customizeLogging(entry -> entry.withEndpoint(name))
        .send()
        .decompress();
  }

  int frequency() {
    return frequency;
  }

  ScheduledExecutorService executor() {
    return executor;
  }
}
