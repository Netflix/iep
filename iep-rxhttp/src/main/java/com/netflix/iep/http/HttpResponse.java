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
package com.netflix.iep.http;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import rx.Observable;

import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class HttpResponse extends HttpMessage<HttpResponse> {

  private final HttpStatus status;

  private HttpResponse(HttpStatus status, SList<HttpHeader> headers, Observable<ByteBuf> content) {
    super(headers, content);
    this.status = status;
  }

  public HttpStatus status() {
    return status;
  }

  public int statusCode() {
    return status.code();
  }

  public HttpResponse withStatus(HttpStatus s) {
    return new HttpResponse(s, headers, content);
  }

  public HttpResponse withHeaders(Iterable<HttpHeader> hs) {
    return new HttpResponse(status, SLists.create(hs), content);
  }

  public HttpResponse withContent(Observable<ByteBuf> obs) {
    return new HttpResponse(status, headers, obs);
  }

  static HttpResponse create(HttpClientResponse<ByteBuf> res) {
    SList<HttpHeader> hs = SLists.empty();
    Iterator<Map.Entry<String, String>> iter = res.headerIterator();
    while (iter.hasNext()) {
      hs = hs.prepend(HttpHeader.create(iter.next()));
    }
    return new HttpResponse(HttpStatus.create(res.getStatus()), hs, res.getContent());
  }
}
