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

import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.iep.guice.GuiceHelper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.HttpURLConnection;
import java.net.URL;

@RunWith(JUnit4.class)
public class KaryonModuleTest {

  @Test
  public void module() throws Exception {
    GuiceHelper helper = new GuiceHelper();
    helper.start(new KaryonModule(), new ArchaiusModule());

    try {
      URL url = new URL("http://localhost:8077/v1/platform/base/env");
      HttpURLConnection con = null;
      try {
        con = (HttpURLConnection) url.openConnection();
        Assert.assertEquals(200, con.getResponseCode());
      } finally {
        if (con != null) {
          con.disconnect();
        }
      }
    } finally {
      helper.shutdown();
    }
  }
}
