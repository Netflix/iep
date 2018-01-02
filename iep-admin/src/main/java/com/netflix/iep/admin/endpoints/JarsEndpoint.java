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
package com.netflix.iep.admin.endpoints;

import com.netflix.iep.admin.HttpEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * List all jars that are on the classpath.
 */
public class JarsEndpoint implements HttpEndpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(JarsEndpoint.class);

  private static final Pattern JAR_PATTERN =
      Pattern.compile("^jar:file:(.+)!/META-INF/MANIFEST.MF$");

  private List<JarInfo> jars;

  /**
   * Create a new instance using the provided class loader to search for jars. Normally
   * this should be {@code Thread.currentThread().getContextClassLoader()}. If listing
   * the resources fails, then and empty list of will be used.
   *
   * @param loader
   *     Class loader to use when searching for jars.
   */
  public JarsEndpoint(ClassLoader loader) {
    try {
      jars = Collections.unmodifiableList(findJars(loader));
    } catch (Exception e) {
      // Debug level is used because this shouldn't impact the actual application
      // and we want to avoid spurious errors for the user. If jars are not showing
      // up on the admin and they are interested, then they can enable debug level
      // for this class.
      LOGGER.debug("failed to list jars in the classpath", e);
      jars = Collections.emptyList();
    }
  }

  /**
   * Create a new instance using the context class loader for the current thread.
   */
  public JarsEndpoint() {
    this(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Get a list of all jars available to the class loader. Jars are detected by looking
   * for all manifest files in the classpath. An attempt will be made to load the manifest
   * to get the build date and version for the jar. If an exception is thrown for the manifest
   * or those attributes are not set in the manifest, then they will be recorded as
   * {@code "-"}.
   *
   * @param loader
   *     Class loader to use when searching for jars.
   * @return
   *     List of all jars found.
   */
  private List<JarInfo> findJars(ClassLoader loader) throws IOException {
    List<JarInfo> jarList = new ArrayList<>();
    Enumeration<URL> urls = loader.getResources("META-INF/MANIFEST.MF");
    while (urls.hasMoreElements()) {
      URL url = urls.nextElement();
      Matcher m = JAR_PATTERN.matcher(url.toString());
      if (m.matches()) {
        String name = m.group(1);
        try (InputStream in = url.openStream()) {
          Manifest manifest = new Manifest(in);
          Attributes attrs = manifest.getMainAttributes();
          String buildDate = attrs.getValue("Build-Date");
          String version = attrs.getValue("Implementation-Version");
          jarList.add(new JarInfo(name, buildDate, version));
        } catch (IOException e) {
          LOGGER.debug("failed to load manifest for " + name, e);
          jarList.add(new JarInfo(name, "-", "-"));
        }
      }
    }
    return jarList;
  }

  /** Returns a list of jars on the classpath. */
  public List<JarInfo> jars() {
    return jars;
  }

  @Override public Object get() {
    return jars;
  }

  @Override public Object get(String path) {
    return null;
  }

  /** Bean representing a jar and associated metadata. */
  public static class JarInfo {
    private final String name;
    private final String buildDate;
    private final String implementationVersion;

    public JarInfo(String name, String buildDate, String implementationVersion) {
      this.name = name;
      this.buildDate = (buildDate == null) ? "-" : buildDate;
      this.implementationVersion = (implementationVersion == null) ? "-" : implementationVersion;
    }

    public String getName() {
      return name;
    }

    public String getBuildDate() {
      return buildDate;
    }

    public String getImplementationVersion() {
      return implementationVersion;
    }

    @Override public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      JarInfo jarInfo = (JarInfo) o;

      if (!name.equals(jarInfo.name)) return false;
      if (!buildDate.equals(jarInfo.buildDate)) return false;
      return implementationVersion.equals(jarInfo.implementationVersion);

    }

    @Override public int hashCode() {
      int result = name.hashCode();
      result = 31 * result + buildDate.hashCode();
      result = 31 * result + implementationVersion.hashCode();
      return result;
    }

    @Override public String toString() {
      return "JarInfo(" + name + ", " + buildDate + ", " + implementationVersion + ")";
    }
  }
}
