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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
    int status;
    byte[] data;
    try {
      status = (obj instanceof ErrorMessage) ? ((ErrorMessage) obj).getStatus() : 200;
      data = JsonEncoder.encode(obj);
    } catch (Exception e) {
      status = 500;
      data = JsonEncoder.encodeUnsafe(new ErrorMessage(status, e));
    }
    Map<String, String> headers = Collections.singletonMap("Content-Type", "application/json");
    return new BasicHttpResponse(status, headers, data);
  }

  /** HTTP status code. */
  int status();

  /** Headers to includ in the response. */
  Map<String, String> headers();

  /** Payload for the response. */
  byte[] entity();

  /**
   * Creates a copy of the response with the entity compressed using GZIP.
   */
  default HttpResponse gzip() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (GZIPOutputStream out = new GZIPOutputStream(baos)) {
      out.write(entity());
    }
    byte[] compressed = baos.toByteArray();

    Map<String, String> headers = new HashMap<>(headers());
    headers.put("Content-Encoding", "gzip");
    return new BasicHttpResponse(status(), Collections.unmodifiableMap(headers), compressed);
  }
}
