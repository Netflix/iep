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

import com.netflix.spectator.sandbox.HttpLogEntry;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpPrincipal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wraps an http handler and provides a common access log and metrics.
 */
class AccessLogHandler implements HttpHandler {

  private final HttpHandler handler;

  AccessLogHandler(HttpHandler handler) {
    this.handler = handler;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    try (Exchange ex = new Exchange(exchange)) {
      handler.handle(ex);
    }

  }

  private static class Exchange extends HttpExchange implements AutoCloseable {
    private final HttpExchange underlying;
    private final HttpLogEntry entry;

    Exchange(HttpExchange underlying) {
      this.underlying = underlying;
      this.entry = new HttpLogEntry();

      //Fill in request attributes
      entry.mark("start");

      entry.withMethod(underlying.getRequestMethod());
      entry.withRequestUri(underlying.getRequestURI());

      String originalUri = "http:/"
          + underlying.getLocalAddress()
          + underlying.getRequestURI();
      entry.withOriginalUri(URI.create(originalUri));

      InetSocketAddress addr = underlying.getRemoteAddress();
      entry.withRemoteAddr(addr.getHostName());
      entry.withRemotePort(addr.getPort());

      String length = underlying.getRequestHeaders().getFirst("Content-Length");
      if (length != null) {
        entry.withRequestContentLength(Long.parseLong(length));
      }

      // Capture request headers
      for (Map.Entry<String, List<String>> header : underlying.getRequestHeaders().entrySet()) {
        String k = header.getKey();
        String vs = header.getValue().stream().collect(Collectors.joining(","));
        entry.withRequestHeader(k, vs);
      }
    }

    @Override
    public Headers getRequestHeaders() {
      return underlying.getRequestHeaders();
    }

    @Override
    public Headers getResponseHeaders() {
      return underlying.getResponseHeaders();
    }

    @Override
    public URI getRequestURI() {
      return underlying.getRequestURI();
    }

    @Override
    public String getRequestMethod() {
      return underlying.getRequestMethod();
    }

    @Override
    public HttpContext getHttpContext() {
      return underlying.getHttpContext();
    }

    @Override
    public void close() {
      entry.mark("complete");
      HttpLogEntry.logServerRequest(entry);
      underlying.close();
    }

    @Override
    public InputStream getRequestBody() {
      return underlying.getRequestBody();
    }

    @Override
    public OutputStream getResponseBody() {
      return underlying.getResponseBody();
    }

    @Override
    public void sendResponseHeaders(int status, long length) throws IOException {
      entry.mark("send-headers");
      entry.withStatusCode(status);

      // Use -1 for chunked and 0 for empty payload, where as length will be -1 for empty
      // and 0 for chunked.
      // https://docs.oracle.com/javase/8/docs/jre/api/net/httpserver/spec/com/sun/net/httpserver/HttpExchange.html#sendResponseHeaders-int-long-
      entry.withResponseContentLength((length < 0) ? 0 : -1);

      // Capture response headers
      for (Map.Entry<String, List<String>> header : underlying.getResponseHeaders().entrySet()) {
        String k = header.getKey();
        String vs = header.getValue().stream().collect(Collectors.joining(","));
        entry.withResponseHeader(k, vs);
      }

      // Forward to underlying to do the actual work
      underlying.sendResponseHeaders(status, length);
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
      return underlying.getRemoteAddress();
    }

    @Override
    public int getResponseCode() {
      return underlying.getResponseCode();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
      return underlying.getLocalAddress();
    }

    @Override
    public String getProtocol() {
      return underlying.getProtocol();
    }

    @Override
    public Object getAttribute(String s) {
      return underlying.getAttribute(s);
    }

    @Override
    public void setAttribute(String s, Object o) {
      underlying.setAttribute(s, o);
    }

    @Override
    public void setStreams(InputStream inputStream, OutputStream outputStream) {
      underlying.setStreams(inputStream, outputStream);
    }

    @Override
    public HttpPrincipal getPrincipal() {
      return underlying.getPrincipal();
    }
  }
}
