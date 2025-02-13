/*
 * Copyright 2014-2025 Netflix, Inc.
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
package com.netflix.iep.spring;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This class can be injected to access the command line arguments passed to the
 * {@link Main} instance.
 */
public class Args implements Iterable<String> {

  /**
   * Create a new instance from an array of command line arguments.
   */
  public static Args from(String[] args) {
    return new Args(Arrays.asList(args));
  }

  private final List<String> argv;

  private Args(List<String> argv) {
    this.argv = argv;
  }

  /** Return the argument at the specified position. */
  public String get(int i) {
    return argv.get(i);
  }

  /** Return true if the list of arguments is empty. */
  public boolean isEmpty() {
    return argv.isEmpty();
  }

  /** Return number of command line arguments. */
  public int size() {
    return argv.size();
  }

  /** Return an array with a copy of the command line arguments. */
  public String[] asArray() {
    return argv.toArray(new String[0]);
  }

  @Override public Iterator<String> iterator() {
    return argv.iterator();
  }

  @Override public String toString() {
    return String.join(" ", argv);
  }
}
