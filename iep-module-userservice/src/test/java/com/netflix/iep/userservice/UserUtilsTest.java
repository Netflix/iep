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
package com.netflix.iep.userservice;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class UserUtilsTest {

  @Test
  public void baseEmailNoChange() throws Exception {
    Assert.assertEquals("foo@example.com", UserUtils.baseAddress("foo@example.com"));
  }

  @Test
  public void baseEmailWithSubAddress() throws Exception {
    Assert.assertEquals("foo@example.com", UserUtils.baseAddress("foo+bar-baz@example.com"));
  }
}
