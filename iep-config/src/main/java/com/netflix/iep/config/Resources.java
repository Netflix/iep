/*
 * Copyright 2014-2021 Netflix, Inc.
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
package com.netflix.iep.config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

final class Resources {

  private Resources() {
  }

  static URL getResource(String name) {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    if (loader == null) {
      loader = Resources.class.getClassLoader();
    }
    URL url = loader.getResource(name);
    if (url == null) {
      throw new IllegalArgumentException("resource not found: " + name);
    }
    return url;
  }

  static String toString(URL url) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    int length;
    try (InputStream in = url.openStream()) {
      while ((length = in.read(buffer)) > 0) {
        baos.write(buffer, 0, length);
      }
    }
    return baos.toString("UTF-8");
  }
}
