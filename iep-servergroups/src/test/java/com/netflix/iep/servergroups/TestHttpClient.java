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
package com.netflix.iep.servergroups;

import com.netflix.spectator.api.NoopRegistry;
import com.netflix.spectator.ipc.IpcLogger;
import com.netflix.spectator.ipc.http.HttpClient;
import com.netflix.spectator.ipc.http.HttpRequestBuilder;
import com.netflix.spectator.ipc.http.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;

class TestHttpClient implements HttpClient {

  private final IpcLogger logger = new IpcLogger(new NoopRegistry());

  private final int status;
  private final byte[] data;

  TestHttpClient(int status, byte[] data) {
    this.status = status;
    this.data = data;
  }

  @Override public HttpRequestBuilder newRequest(URI uri) {
    return new HttpRequestBuilder(logger, uri) {
      protected HttpResponse sendImpl() throws IOException {
        return new HttpResponse(status, Collections.emptyMap(), data);
      }
    };
  }

  static HttpClient empty(int status) {
    return new TestHttpClient(status, new byte[] {});
  }

  static HttpClient resource(int status, String name) throws IOException {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    try (InputStream in = cl.getResourceAsStream(name)) {
      return new TestHttpClient(status, readAll(in));
    }
  }

  private static byte[] readAll(InputStream in) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buf = new byte[4096];
    int length;
    while ((length = in.read(buf)) > 0) {
      baos.write(buf, 0, length);
    }
    return baos.toByteArray();
  }
}
