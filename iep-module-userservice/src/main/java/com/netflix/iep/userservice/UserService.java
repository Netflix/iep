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

import java.util.Locale;
import java.util.Set;

/**
 * Service for tracking the set of valid users (email addresses) available in the
 * environment. This is typically used for validation in use-cases where we should
 * only have known good addresses.
 */
public interface UserService {

  /** Returns the set of all email addresses. */
  Set<String> emailAddresses();

  /**
   * Checks to see if an email address is valid for this service.
   *
   * @param email
   *     The email address to check. This may not be an exact match for an address
   *     in the set returned by {@link #emailAddresses()}. In particular,
   *     <a href="https://tools.ietf.org/html/rfc5233#section-4">subaddresses</a>
   *     will not be in the set, but will be indicated as valid.
   * @return
   *     True if the email is valid based on the set of available addresses for this
   *     service.
   */
  default boolean isValidEmail(String email) {
    return emailAddresses().contains(UserUtils.baseAddress(email.toLowerCase(Locale.US)));
  }

  /**
   * Attempts to convert a given email address to a valid address for this service. By
   * default it will simply check if the provided email is valid and return it without
   * modification. Some implementations may also support replacing old mailing lists or
   * departed users with replacements.
   *
   * @param email
   *     The email address to convert.
   * @return
   *     Valid email or null if none could be found.
   */
  default String toValidEmail(String email) {
    return isValidEmail(email) ? email : null;
  }
}
