/*
 * Copyright 2014-2022 Netflix, Inc.
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

import java.time.Duration;

public class AdminConfigTest {

  @Test
  public void defaultPort() {
    Assert.assertEquals(8077, AdminConfig.DEFAULT.port());
  }

  @Test
  public void defaultBacklog() {
    Assert.assertEquals(10, AdminConfig.DEFAULT.backlog());
  }

  @Test
  public void defaultShutdownDelay() {
    Assert.assertEquals(Duration.ZERO, AdminConfig.DEFAULT.shutdownDelay());
  }

  @Test
  public void defaultUiLocation() {
    Assert.assertEquals("/ui", AdminConfig.DEFAULT.uiLocation());
  }
}
