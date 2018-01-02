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

import java.util.Map;

/**
 * Minimal implementation of a response.
 */
class BasicHttpResponse implements HttpResponse {

  private final int status;
  private final Map<String, String> headers;
  private final byte[] entity;

  BasicHttpResponse(int status, Map<String, String> headers, byte[] entity) {
    this.status = status;
    this.headers = headers;
    this.entity = entity;
  }

  @Override
  public int status() {
    return status;
  }

  @Override
  public Map<String, String> headers() {
    return headers;
  }

  @Override
  public byte[] entity() {
    return entity;
  }
}
