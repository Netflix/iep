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

import com.fasterxml.jackson.core.type.TypeReference;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Collects mappings from whitepages departed endpoint. The valid email addresses
 * returned are the values associated with the mapping.
 */
@Singleton
final class DepartedUserService extends AbstractUserService {

  private static final TypeReference<Map<String, String>> MAP_TYPE =
      new TypeReference<Map<String, String>>() {};

  private final AtomicReference<Map<String, String>> mapping =
      new AtomicReference<>(Collections.emptyMap());

  @Inject
  DepartedUserService(Context context) {
    super(context, "departed");
  }

  @Override protected Set<String> parseResponse(byte[] data) throws IOException {
    Map<String, String> vs = context.objectMapper().readValue(data, MAP_TYPE);
    mapping.set(vs);
    return Collections.unmodifiableSet(new TreeSet<>(vs.values()));
  }

  @Override public String toValidEmail(String email) {
    return isValidEmail(email)
        ? email
        : mapping.get().get(UserUtils.baseAddress(email));
  }
}
