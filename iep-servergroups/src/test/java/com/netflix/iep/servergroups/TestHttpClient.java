/*
 * Copyright 2014-2024 Netflix, Inc.
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
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

class TestHttpClient implements HttpClient {

  private final IpcLogger logger = new IpcLogger(new NoopRegistry());

  private final int status;
  private final byte[] data;
  private final boolean gzip;

  TestHttpClient(int status, byte[] data, boolean gzip) {
    this.status = status;
    this.data = data;
    this.gzip = gzip;
  }

  @Override public HttpRequestBuilder newRequest(URI uri) {
    return new HttpRequestBuilder(logger, uri) {
      protected HttpResponse sendImpl() throws IOException {
        Map<String, List<String>> headers = gzip
            ? Collections.singletonMap("Content-Encoding", Collections.singletonList("gzip"))
            : Collections.emptyMap();
        return new HttpResponse(status, headers, data);
      }
    };
  }

  static HttpClient empty(int status) {
    return new TestHttpClient(status, new byte[] {}, false);
  }

  static HttpClient resource(int status, String name, boolean gzip) throws IOException {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    try (InputStream in = cl.getResourceAsStream(name)) {
      return new TestHttpClient(status, gzip ? compress(readAll(in)) : readAll(in), gzip);
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

  private static byte[] compress(byte[] data) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length);
    try (GZIPOutputStream out = new GZIPOutputStream(baos)) {
      out.write(data);
    }
    return baos.toByteArray();
  }
}
