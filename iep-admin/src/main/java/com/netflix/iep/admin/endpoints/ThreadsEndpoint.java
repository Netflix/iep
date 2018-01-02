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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Endpoint for providing access to environment variables.
 */
public class ThreadsEndpoint implements HttpEndpoint {

  /**
   * Helper to get all the stack traces and remove entries that may be null. The
   * {@code Thread.getAllStackTraces()} call has race conditions which can lead
   * to null keys or values in the result map. For some reason these seem to be
   * more likely to occur on travis.
   */
  static Map<Thread, StackTraceElement[]> getAllStackTraces() {
    Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
    return threads.entrySet()
        .stream()
        .filter(e -> e.getKey() != null && e.getValue() != null)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override public Object get() {
    return threads();
  }

  @Override public Object get(String path) {
    Pattern p = Pattern.compile(path);
    return threads().stream()
        .filter(t -> p.matcher(t.getName()).find())
        .collect(Collectors.toList());
  }

  private List<ThreadInfo> threads() {
    return getAllStackTraces().entrySet()
        .stream()
        .map(e -> new ThreadInfo(e.getKey(), e.getValue()))
        .collect(Collectors.toList());
  }

  public static class ThreadInfo {
    private final String group;
    private final String name;
    private final String state;
    private final int priority;
    private final long id;
    private final List<String> stackTrace;

    public ThreadInfo(Thread t, StackTraceElement[] stack) {
      // Thread group will be null if the thread has stopped before this line executes
      ThreadGroup tg = t.getThreadGroup();
      group = (tg != null) ? tg.getName() : "null";
      name = t.getName();
      state = t.getState().name();
      priority = t.getPriority();
      id = t.getId();
      stackTrace = new ArrayList<>();
      for (StackTraceElement e : stack) {
        if (e.getLineNumber() < 0) {
          String s = String.format("%s.%s(Native Method)",
              e.getClassName(),
              e.getMethodName());
          stackTrace.add(s);
        } else {
          String s = String.format("%s.%s(%s:%d)",
              e.getClassName(),
              e.getMethodName(),
              e.getFileName(),
              e.getLineNumber());
          stackTrace.add(s);
        }
      }
    }

    public String getGroup() {
      return group;
    }

    public String getName() {
      return name;
    }

    public String getState() {
      return state;
    }

    public int getPriority() {
      return priority;
    }

    public long getId() {
      return id;
    }

    public List<String> getStackTrace() {
      return stackTrace;
    }

    @Override public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ThreadInfo that = (ThreadInfo) o;

      if (priority != that.priority) return false;
      if (id != that.id) return false;
      if (!group.equals(that.group)) return false;
      if (!name.equals(that.name)) return false;
      if (!state.equals(that.state)) return false;
      return stackTrace.equals(that.stackTrace);
    }

    @Override public int hashCode() {
      int result = group.hashCode();
      result = 31 * result + name.hashCode();
      result = 31 * result + state.hashCode();
      result = 31 * result + priority;
      result = 31 * result + (int) (id ^ (id >>> 32));
      result = 31 * result + stackTrace.hashCode();
      return result;
    }

    @Override public String toString() {
      return "ThreadInfo(" + id +
          ", " + name +
          ", " + group +
          ", " + priority +
          ", " + state + ")";
    }
  }
}
