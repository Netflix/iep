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

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * Request handler to map {@link HttpEndpoint} implemenations to a request/response on
 * the server.
 */
class RequestHandler implements HttpHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandler.class);

  private final String path;
  private final HttpEndpoint endpoint;

  RequestHandler(String path, HttpEndpoint endpoint) {
    this.path = path;
    this.endpoint = endpoint;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    // Get everything after the path prefix used for the context
    String reqPath = exchange.getRequestURI().getPath();
    String id = (path.length() == reqPath.length()) ? null : reqPath.substring(path.length());

    if (id != null && !id.startsWith("/")) {
      // Should start with a '/', otherwise return a 404
      sendResponse(exchange, new ErrorMessage(404, reqPath));
    } else {
      // Get id, everything after the starting '/'
      id = (id == null || "/".equals(id)) ? null : id.substring(1);
      try {
        Object obj = (id == null) ? endpoint.get() : endpoint.get(id);
        if (obj == null)
          sendResponse(exchange, new ErrorMessage(404, reqPath));
        else
          handleImpl(exchange, obj);
      } catch (HttpException e) {
        LOGGER.debug("request failed: " + reqPath, e);
        sendResponse(exchange, new ErrorMessage(e.getStatus(), e.getCause()));
      } catch (IllegalArgumentException | IllegalStateException e) {
        LOGGER.debug("request failed: " + reqPath, e);
        sendResponse(exchange, new ErrorMessage(400, e));
      } catch (Exception e) {
        LOGGER.debug("request failed: " + reqPath, e);
        sendResponse(exchange, new ErrorMessage(500, e));
      }
    }
  }

  private void handleImpl(HttpExchange exchange, Object obj) throws IOException {
    addCorsHeaders(exchange);
    switch (exchange.getRequestMethod()) {
      case "OPTIONS":
        exchange.sendResponseHeaders(200, -1L);
        break;
      case "GET":
        sendResponse(exchange, obj);
        break;
      case "HEAD":
        sendResponse(exchange, obj);
        break;
      default:
        // Return method not allowed error for all other method types
        sendResponse(exchange, new ErrorMessage(405, "only OPTIONS, GET, and HEAD are supported"));
        break;
    }
  }

  private boolean shouldCompressResponse(Headers reqHeaders) {
    String accept = reqHeaders.getFirst("Accept-Encoding");
    return accept != null && accept.contains("gzip");
  }

  private void sendResponse(HttpExchange exchange, Object obj) throws IOException {
    HttpResponse res = HttpResponse.create(obj);

    Headers reqHeaders = exchange.getRequestHeaders();
    if (shouldCompressResponse(reqHeaders)) {
      res = res.gzip();
    }

    Headers resHeaders = exchange.getResponseHeaders();
    for (Map.Entry<String, String> entry : res.headers().entrySet()) {
      resHeaders.add(entry.getKey(), entry.getValue());
    }

    if ("HEAD".equals(exchange.getRequestMethod())) {
      exchange.sendResponseHeaders(res.status(), -1L);
    } else {
      exchange.sendResponseHeaders(res.status(), res.entity().length);
      try (OutputStream out = exchange.getResponseBody()) {
        out.write(res.entity());
      }
    }
  }

  private void addCorsHeaders(HttpExchange exchange) {
    Headers reqHeaders = exchange.getRequestHeaders();
    String origin = reqHeaders.getFirst("Origin");
    origin = (origin == null) ? "*" : origin;

    Headers resHeaders = exchange.getResponseHeaders();
    resHeaders.add("Access-Control-Allow-Origin", origin);
    resHeaders.add("Access-Control-Allow-Methods", "GET, HEAD");
  }
}
