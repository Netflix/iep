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
import io.netty.handler.codec.http.HttpMethod;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import rx.Observable;

import java.net.URI;
import java.util.Collections;
import java.util.List;

public final class HttpRequest extends HttpMessage<HttpRequest> {

  private final HttpMethod method;
  private final URI uri;

  private HttpRequest(HttpMethod method, URI uri) {
    this(method, uri, SLists.<HttpHeader>empty(), Observable.<ByteBuf>empty());
  }

  private HttpRequest(HttpMethod method, URI uri, SList<HttpHeader> headers, Observable<ByteBuf> content) {
    super(headers, content);
    this.method = method;
    this.uri = uri;
  }

  public HttpMethod method() {
    return method;
  }

  public URI uri() {
    return uri;
  }

  public HttpRequest withMethod(HttpMethod m) {
    return new HttpRequest(m, uri, headers, content);
  }

  public HttpRequest withUri(URI u) {
    return new HttpRequest(method, u, headers, content);
  }

  public HttpRequest withUri(String u) {
    return withUri(URI.create(u));
  }

  public HttpRequest withHeaders(Iterable<HttpHeader> hs) {
    return new HttpRequest(method, uri, SLists.create(hs), content);
  }

  public HttpRequest withContent(Observable<ByteBuf> obs) {
    return new HttpRequest(method, uri, headers, obs);
  }

  public HttpRequest compress() {
    return this;
  }

  public static HttpRequest create(HttpMethod method, String uri) {
    return new HttpRequest(method, URI.create(uri));
  }

  public static HttpRequest create(HttpMethod method, URI uri) {
    return new HttpRequest(method, uri);
  }

  public static HttpRequest createGet(String uri) {
    return new HttpRequest(HttpMethod.GET, URI.create(uri));
  }

  public static HttpRequest createGet(URI uri) {
    return new HttpRequest(HttpMethod.GET, uri);
  }

  public static HttpRequest createPost(String uri) {
    return new HttpRequest(HttpMethod.POST, URI.create(uri));
  }

  public static HttpRequest createPost(URI uri) {
    return new HttpRequest(HttpMethod.POST, uri);
  }

  public static HttpRequest createPut(String uri) {
    return new HttpRequest(HttpMethod.PUT, URI.create(uri));
  }

  public static HttpRequest createPut(URI uri) {
    return new HttpRequest(HttpMethod.PUT, uri);
  }

  public static HttpRequest createDelete(String uri) {
    return new HttpRequest(HttpMethod.DELETE, URI.create(uri));
  }

  public static HttpRequest createDelete(URI uri) {
    return new HttpRequest(HttpMethod.DELETE, uri);
  }

  static HttpRequest create(HttpClientRequest<ByteBuf, ByteBuf> req) {
    return new HttpRequest(
        req.getMethod(),
        URI.create(req.getUri()),
        SLists.<HttpHeader>empty(), // TODO
        Observable.<ByteBuf>empty());
  }
}
