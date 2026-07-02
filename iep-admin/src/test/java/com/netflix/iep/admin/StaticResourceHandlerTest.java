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
package com.netflix.iep.admin;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class StaticResourceHandlerTest {

  @Test
  public void safePaths() {
    Assert.assertTrue(StaticResourceHandler.isSafeResource("static/index.html"));
    Assert.assertTrue(StaticResourceHandler.isSafeResource("static/css/app.css"));
    Assert.assertTrue(StaticResourceHandler.isSafeResource("static/test.txt"));
    // A dot within a segment is fine, only a whole `.`/`..` segment is a traversal.
    Assert.assertTrue(StaticResourceHandler.isSafeResource("static/..foo/a.b.c.js"));
  }

  @Test
  public void parentTraversalRejected() {
    Assert.assertFalse(StaticResourceHandler.isSafeResource("static/../application.conf"));
    Assert.assertFalse(StaticResourceHandler.isSafeResource("static/../../etc/passwd"));
    Assert.assertFalse(StaticResourceHandler.isSafeResource(".."));
    Assert.assertFalse(StaticResourceHandler.isSafeResource("../static/index.html"));
    Assert.assertFalse(StaticResourceHandler.isSafeResource("static/../"));
  }

  @Test
  public void currentDirSegmentRejected() {
    Assert.assertFalse(StaticResourceHandler.isSafeResource("./static/index.html"));
    Assert.assertFalse(StaticResourceHandler.isSafeResource("static/./index.html"));
  }

  @Test
  public void backslashRejected() {
    Assert.assertFalse(StaticResourceHandler.isSafeResource("static\\..\\application.conf"));
  }

  @Test
  public void nullByteRejected() {
    Assert.assertFalse(StaticResourceHandler.isSafeResource("static/index.html\u0000.png"));
  }
}
