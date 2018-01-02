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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper functions for working with users.
 */
final class UserUtils {
  private UserUtils() {
  }

  private static final Pattern SUB_ADDRESS = Pattern.compile("^([^+]+)\\+[^@]+(@.*)$");

  /**
   * Return the base address without the detail portion. For example
   * {@code foo+bar@example.com} would return {@code foo@example.com}.
   */
  static String baseAddress(String email) {
    Matcher m = SUB_ADDRESS.matcher(email);
    return m.matches() ? m.group(1) + m.group(2) : email;
  }
}
