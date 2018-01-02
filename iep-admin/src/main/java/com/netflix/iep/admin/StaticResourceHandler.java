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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serves up static resources from the classpath.
 */
class StaticResourceHandler implements HttpHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(StaticResourceHandler.class);

  private static final Pattern FILE_EXT = Pattern.compile("^.*\\.([a-zA-Z0-9]+)$");

  private static final Map<String, String> FILE_TYPES = new HashMap<>();

  static {
    FILE_TYPES.put("txt",  "text/plain");
    FILE_TYPES.put("html", "text/html");
    FILE_TYPES.put("css",  "text/css");
    FILE_TYPES.put("js",   "application/x-javascript");
    FILE_TYPES.put("json", "application/json");
    FILE_TYPES.put("png",  "image/png");
    FILE_TYPES.put("jpg",  "image/jpeg");
    FILE_TYPES.put("gif",  "image/gif");
  }

  private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

  private final ClassLoader classLoader;
  private final NavigableMap<String, String> singlePagePaths;

  StaticResourceHandler(ClassLoader classLoader, Map<String, String> singlePagePaths) {
    this.classLoader = classLoader;
    this.singlePagePaths = new TreeMap<>(singlePagePaths).descendingMap();
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    String path = exchange.getRequestURI().getPath();
    for (Map.Entry<String, String> entry : singlePagePaths.entrySet()) {
      if (path.startsWith(entry.getKey())) {
        LOGGER.debug("mapping {} to {}", path, entry.getValue());
        path = "/" + entry.getValue();
      }
    }

    exchange.getResponseHeaders().add("Content-Type", getContentType(path));

    String resource = path.substring(1);
    LOGGER.debug("loading resource {}", resource);
    try (InputStream in = classLoader.getResourceAsStream(resource)) {
      if (in == null) {
        exchange.sendResponseHeaders(404, -1);
      } else {
        exchange.sendResponseHeaders(200, 0);
        try (OutputStream out = exchange.getResponseBody()) {
          byte[] buffer = new byte[4096];
          int len;
          while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
          }
        } catch (IOException e) {
          e.printStackTrace();
          LOGGER.debug("failed to write resource " + resource, e);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      LOGGER.debug("failed to serve resource " + resource, e);
    }
  }

  String getExtension(String name) {
    Matcher m = FILE_EXT.matcher(name);
    return m.matches() ? m.group(1) : null;
  }

  String getContentType(String name) {
    String ext = getExtension(name);
    if (ext == null) {
      return DEFAULT_CONTENT_TYPE;
    } else {
      String ctype = FILE_TYPES.get(ext);
      return (ctype == null) ? DEFAULT_CONTENT_TYPE : ctype;
    }
  }
}
