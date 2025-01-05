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
package com.netflix.iep.admin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * HTTP response to return to the user.
 */
public interface HttpResponse {

  /** Create a new response from an object. */
  static HttpResponse create(Object obj) {
    return (obj instanceof HttpResponse) ? (HttpResponse) obj : json(obj);
  }

  /** Create a new response by JSON encoding the provided object. */
  static HttpResponse json(Object obj) {
    int status = (obj instanceof ErrorMessage) ? ((ErrorMessage) obj).getStatus() : 200;
    HttpEntity entity = out -> JsonEncoder.encode(obj, out);
    Map<String, String> headers = Collections.singletonMap("Content-Type", "application/json");
    return new BasicHttpResponse(status, headers, entity);
  }

  /** HTTP status code. */
  int status();

  /** Headers to includ in the response. */
  Map<String, String> headers();

  /** Write entity to the provided stream. */
  void writeEntity(OutputStream out) throws IOException;

  /** Payload for the response. */
  default byte[] entity() {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      writeEntity(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Creates a copy of the response with the entity compressed using GZIP.
   */
  default HttpResponse gzip() throws IOException {
    HttpEntity entity = out -> {
      try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
        writeEntity(gzip);
      }
    };
    Map<String, String> headers = new HashMap<>(headers());
    headers.put("Content-Encoding", "gzip");
    return new BasicHttpResponse(status(), Collections.unmodifiableMap(headers), entity);
  }
}
