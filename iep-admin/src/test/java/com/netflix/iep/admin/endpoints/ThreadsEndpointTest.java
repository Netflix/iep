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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.PatternSyntaxException;


@RunWith(JUnit4.class)
public class ThreadsEndpointTest {

  private final ThreadsEndpoint endpoint = new ThreadsEndpoint();

  @Test @SuppressWarnings("unchecked")
  public void get() {
    List<ThreadsEndpoint.ThreadInfo> infos = (List<ThreadsEndpoint.ThreadInfo>) endpoint.get();

    Map<Thread, StackTraceElement[]> threads = ThreadsEndpoint.getAllStackTraces();
    Assert.assertEquals(threads.size(), infos.size());
    for (Map.Entry<Thread, StackTraceElement[]> entry : threads.entrySet()) {
      Thread t = entry.getKey();
      Optional<ThreadsEndpoint.ThreadInfo> match = infos.stream()
          .filter(i -> t.getId() == i.getId())
          .findFirst();
      Assert.assertTrue(match.isPresent());
      Assert.assertEquals(t.getName(), match.get().getName());
      Assert.assertEquals(t.getPriority(), match.get().getPriority());
      // Group, state, and stack trace can change, so they aren't checked here
    }
  }

  @Test @SuppressWarnings("unchecked")
  public void getWithPath() {
    List<ThreadsEndpoint.ThreadInfo> infos =
        (List<ThreadsEndpoint.ThreadInfo>) endpoint.get("main");

    Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
    int expected = (int) threads.keySet()
        .stream()
        .filter(t -> t.getName().contains("main"))
        .count();

    Assert.assertEquals(expected, infos.size());
  }

  @Test @SuppressWarnings("unchecked")
  public void getWithPathEmpty() {
    // This assumes no matches for the pattern
    List<ThreadsEndpoint.ThreadInfo> infos =
        (List<ThreadsEndpoint.ThreadInfo>) endpoint.get("ZZZZZZZZZZZZZZZZZZZZZZZZ");
    Assert.assertEquals(0, infos.size());
  }

  @Test(expected = PatternSyntaxException.class)
  public void getWithPathBadRegex() {
    endpoint.get("(");
  }
}
