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
package com.netflix.iep.karyon3;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Singleton
class JarHandler extends SimpleHandler {

  private static final Pattern JAR_PATTERN =
      Pattern.compile("^jar:file:(.+)!/META-INF/MANIFEST.MF$");

  @Inject
  JarHandler(ObjectMapper mapper) {
    super(mapper);
  }

  @Override
  protected Object get() {
    List<Map<String, String>> jars = new ArrayList<>();
    try {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      Enumeration<URL> urls = cl.getResources("META-INF/MANIFEST.MF");
      while (urls.hasMoreElements()) {
        URL url = urls.nextElement();
        Matcher m = JAR_PATTERN.matcher(url.toString());
        if (m.matches()) {
          Map<String, String> jarInfo = new HashMap<>();
          jarInfo.put("name", m.group(1));
          jars.add(jarInfo);
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

    // TreeMap is used for so user will conveniently get sorted keys, but is not required.
    Map<String, Object> wrapper = new TreeMap<>();
    wrapper.put("jars", jars);
    return wrapper;
  }
}
