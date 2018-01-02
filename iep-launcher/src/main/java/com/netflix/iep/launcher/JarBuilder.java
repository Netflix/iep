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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class JarBuilder {

  private String workingDir = null;
  private String mainClass = null;
  private boolean cleanWorkingDir = true;
  private File outputFile = null;
  private final List<File> jars = new ArrayList<>();

  public JarBuilder withMainClass(String mainClass) {
    this.mainClass = mainClass;
    return this;
  }

  public JarBuilder withWorkingDir(String workingDir) {
    this.workingDir = workingDir;
    return this;
  }

  public JarBuilder shouldCleanWorkingDir(boolean b) {
    this.cleanWorkingDir = b;
    return this;
  }

  public JarBuilder withOutputFile(File f) {
    this.outputFile = f;
    return this;
  }

  public JarBuilder addJar(File jar) {
    jars.add(jar);
    return this;
  }

  public JarBuilder addJars(File... jar) {
    for (File f : jar) {
      addJar(f);
    }
    return this;
  }

  public JarBuilder addJars(Iterable<File> jar) {
    for (File f : jar) {
      addJar(f);
    }
    return this;
  }

  private void addEntry(ZipOutputStream out, String name, String content) throws IOException {
    ZipEntry entry = new ZipEntry(name);
    out.putNextEntry(entry);
    out.write(content.getBytes("UTF-8"));
    out.closeEntry();
  }

  private void addEntry(ZipOutputStream out, String name, InputStream in) throws IOException {
    ZipEntry entry = new ZipEntry(name);
    out.putNextEntry(entry);
    byte[] buf = new byte[4096];
    int length;
    while ((length = in.read(buf)) > 0) {
      out.write(buf, 0, length);
    }
    out.closeEntry();
  }

  private void addManifest(ZipOutputStream out) throws IOException {
    addEntry(out, "META-INF/MANIFEST.MF", "Main-Class: com.netflix.iep.launcher.Main\n");
  }

  private File getLocation() throws Exception {
    URI uri = JarBuilder.class.getProtectionDomain().getCodeSource().getLocation().toURI();
    return new File(uri.getPath());
  }

  private void addLauncherClasses(ZipOutputStream out) throws Exception {
    File loc = getLocation();
    if (loc.isDirectory()) {
      File pkg = new File(loc, "com/netflix/iep/launcher");
      File[] classes = pkg.listFiles((dir, name) -> name.endsWith(".class"));
      for (File cls : classes) {
        try (FileInputStream in = new FileInputStream(cls)) {
          addEntry(out, "com/netflix/iep/launcher/" + cls.getName(), in);
        }
      }
    } else {
      try (ZipFile zf = new ZipFile(loc)) {
        Enumeration<? extends ZipEntry> entries = zf.entries();
        while (entries.hasMoreElements()) {
          ZipEntry entry = entries.nextElement();
          String n = entry.getName();
          if (n.startsWith("com/netflix/iep/launcher/") && n.endsWith(".class")) {
            try (InputStream in = zf.getInputStream(entry)) {
              addEntry(out, n, in);
            }
          }
        }
      }
    }
  }

  private void addConfig(ZipOutputStream out) throws IOException {
    Properties props = new Properties();
    props.setProperty(Settings.MAIN_CLASS, mainClass);
    if (workingDir != null) {
      props.setProperty(Settings.WORKING_DIR, workingDir);
    }
    props.setProperty(Settings.CLEAN_WORKING_DIR, "" + cleanWorkingDir);

    ZipEntry entry = new ZipEntry("launcher.properties");
    out.putNextEntry(entry);
    props.store(out, "launcher settings");
    out.closeEntry();
  }

  private void addJarsToArchive(ZipOutputStream out) throws IOException {
    for (File jar : jars) {
      try (FileInputStream in = new FileInputStream(jar)) {
        addEntry(out, jar.getName(), in);
      }
    }
  }

  public void build() throws Exception {
    try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outputFile))) {
      addManifest(out);
      addLauncherClasses(out);
      addConfig(out);
      addJarsToArchive(out);
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length < 3) {
      System.err.println("Usage: JarBuilder <output-jar> <main-class> <jar1> ... <jarN>");
      System.exit(1);
    }

    File[] jars = new File[args.length - 2];
    for (int i = 0; i < jars.length; ++i) {
      jars[i] = new File(args[i + 2]);
    }

    new JarBuilder()
        .withOutputFile(new File(args[0]))
        .withMainClass(args[1])
        .addJars(jars)
        .build();
  }
}
