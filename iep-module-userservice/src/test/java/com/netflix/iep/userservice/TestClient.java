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
package com.netflix.iep.userservice;

import com.netflix.spectator.sandbox.HttpClient;
import com.netflix.spectator.sandbox.HttpRequestBuilder;
import com.netflix.spectator.sandbox.HttpResponse;

import java.io.IOException;
import java.net.URI;

class TestClient implements HttpClient {

  private final ResponseSupplier supplier;

  TestClient(ResponseSupplier supplier) {
    this.supplier = supplier;
  }

  @Override public HttpRequestBuilder newRequest(String clientName, URI uri) {
    return new HttpRequestBuilder(clientName, uri) {
      @Override protected HttpResponse sendImpl() throws IOException {
        return supplier.get();
      }
    };
  }

  interface ResponseSupplier {
    HttpResponse get() throws IOException;
  }
}
