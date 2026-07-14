/*
 * Copyright 2014-2026 Netflix, Inc.
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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@RunWith(JUnit4.class)
public class JarBuilderTest {

  @Rule
  public final TemporaryFolder folder = new TemporaryFolder();

  private Set<String> entryNames(File jar) throws Exception {
    Set<String> names = new HashSet<>();
    try (ZipFile zf = new ZipFile(jar)) {
      Enumeration<? extends ZipEntry> entries = zf.entries();
      while (entries.hasMoreElements()) {
        names.add(entries.nextElement().getName());
      }
    }
    return names;
  }

  private File dep(String name) throws Exception {
    File dep = new File(folder.getRoot(), name);
    Files.write(dep.toPath(), "dep".getBytes(StandardCharsets.UTF_8));
    return dep;
  }

  @Test
  public void emptyClasspathElementsAreIgnored() throws Exception {
    File dep = dep("dep.jar");
    File out = new File(folder.getRoot(), "out.jar");

    // Empty entries (from a leading/trailing/repeated separator when a classpath
    // string is split) must be skipped rather than failing with FileNotFoundException.
    JarBuilder.main(new String[] {
        out.getAbsolutePath(), "com.example.Main", "", dep.getAbsolutePath(), ""
    });

    Assert.assertTrue("output jar should be created", out.exists());
    Set<String> names = entryNames(out);
    Assert.assertTrue("real dependency jar should be bundled", names.contains("dep.jar"));
    Assert.assertFalse("no empty-named entry should be added", names.contains(""));
  }

  @Test(expected = FileNotFoundException.class)
  public void nonBlankMissingPathStillFails() throws Exception {
    File out = new File(folder.getRoot(), "out.jar");

    // Only empty tokens are skipped; a non-blank path that does not exist must still
    // fail loudly so a genuinely missing jar is not silently dropped.
    JarBuilder.main(new String[] {
        out.getAbsolutePath(), "com.example.Main",
        new File(folder.getRoot(), "does-not-exist.jar").getAbsolutePath()
    });
  }
}
