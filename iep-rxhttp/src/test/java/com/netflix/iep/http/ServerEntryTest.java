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
package com.netflix.iep.http;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(JUnit4.class)
public class ServerEntryTest {

  @Test
  public void emptyList() {
    ServerEntry entry = new ServerEntry(Collections.<Server>emptyList(), 0L);
    Assert.assertTrue(entry.next(1).isEmpty());
  }

  @Test
  public void overflow() throws Exception {
    // Need at least 2 entries to get negative results from mod
    List<Server> servers = new ArrayList<>();
    servers.add(new Server("foo", 7001, false));
    servers.add(new Server("bar", 7001, false));
    ServerEntry entry = new ServerEntry(servers, 0L);

    // Set iteration to just before int max value
    Field f = ServerEntry.class.getDeclaredField("nextPos");
    f.setAccessible(true);
    ((AtomicInteger) f.get(entry)).set(Integer.MAX_VALUE - 2);

    List<Server> results = entry.next(5);
    Assert.assertEquals(5, results.size());
  }
}
