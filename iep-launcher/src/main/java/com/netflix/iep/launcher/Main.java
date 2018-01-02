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
package com.netflix.iep.launcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class Main {

  private static boolean loggingEnabled = false;

  private static void log(String message) {
    if (loggingEnabled) {
      Calendar c = Calendar.getInstance();
      System.out.printf("[%1$tFT%1$tT] %2$s%n", c, message);
    }
  }

  private static void loadConfig() throws Exception {
    final ClassLoader cl = Main.class.getClassLoader();
    try (InputStream in = cl.getResourceAsStream("launcher.properties")) {
      Properties props = new Properties();
      props.load(in);
      for (String k : props.stringPropertyNames()) {
        // To allow overrides to be set via the command line with -D, only update the system
        // property if the key is not already set.
        if (System.getProperty(k) == null) {
          System.setProperty(k, props.getProperty(k));
        }
      }
    }
  }

  private static File getLocation() throws Exception {
    URI uri = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI();
    return new File(uri.getPath());
  }

  private static void extractJar(File jar, File dir) throws Exception {
    try (ZipFile zf = new ZipFile(jar)) {
      byte[] buf = new byte[4096];
      Enumeration<? extends ZipEntry> entries = zf.entries();
      while (entries.hasMoreElements()) {
        ZipEntry ze = entries.nextElement();
        File f = new File(dir, ze.getName());
        if (!ze.isDirectory() && ze.getName().endsWith(".jar") && !f.exists()) {
          try (InputStream in = zf.getInputStream(ze); OutputStream out = new FileOutputStream(f)) {
            int length;
            while ((length = in.read(buf)) > 0) {
              out.write(buf, 0, length);
            }
          }
        } else if (ze.getName().endsWith(".jar") && f.exists()) {
          log("not extracting because jar already exists: " + f);
        } else {
          log("skipping: " + ze.getName());
        }
      }
    }
  }

  private static void delete(File file) {
    if (file.isDirectory()) {
      final File[] fs = file.listFiles();
      if (fs != null) {
        for (File f : fs) {
          delete(f);
        }
      }
    }
    if (file.delete()) {
      log("deleted file: " + file);
    } else {
      log("failed to delete file: " + file);
    }
  }

  private static File extract(File loc) throws Exception {
    boolean clean = Boolean.parseBoolean(System.getProperty(Settings.CLEAN_WORKING_DIR, "true"));

    if (loc.isDirectory()) {
      log("already extracted, using directory: " + loc);
      return loc;
    } else {
      String dflt = System.getProperty("user.home") + "/.iep-launcher/" + loc.getName();
      String path = System.getProperty(Settings.WORKING_DIR);
      path = (path == null) ? dflt : path;
      File dir = new File(path);

      if (clean && dir.isDirectory()) {
        log("already extracted, but " + Settings.CLEAN_WORKING_DIR + "=true, deleting " + dir);
        delete(dir);
      }

      dir.mkdirs();
      extractJar(loc, dir);
      return dir;
    }
  }

  public static void main(String[] args) throws Exception {
    loadConfig();
    loggingEnabled = Boolean.parseBoolean(System.getProperty(Settings.LOGGING_ENABLED, "false"));

    File loc = extract(getLocation());

    File[] files = loc.listFiles((dir, name) -> name.endsWith(".jar"));

    URL[] urls = new URL[files.length];
    for (int i = 0; i < files.length; ++i) {
      urls[i] = files[i].toURI().toURL();
      log("adding jar to classpath: " + urls[i]);
    }

    URLClassLoader cl = URLClassLoader.newInstance(urls, Main.class.getClassLoader());
    Thread.currentThread().setContextClassLoader(cl);
    Class<?> c = cl.loadClass(System.getProperty(Settings.MAIN_CLASS));
    Method m = c.getMethod("main", String[].class);
    m.invoke(null, (Object) args);
  }
}
