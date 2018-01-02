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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Collects emails from whitepages employee endpoint.
 */
@Singleton
final class EmployeeUserService extends AbstractUserService {

  private static final TypeReference<List<User>> LIST_TYPE = new TypeReference<List<User>>() {};

  @Inject
  EmployeeUserService(Context context) {
    super(context, "employee");
  }


  @Override protected Set<String> parseResponse(byte[] data) throws IOException {
    List<User> vs = context.objectMapper().readValue(data, LIST_TYPE);
    Set<String> es = vs.stream()
        .filter(User::isValid)
        .map(User::getEmail)
        .collect(Collectors.toSet());
    return Collections.unmodifiableSet(new TreeSet<>(es));
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class User {
    private String email;
    private boolean active = true;

    User() {
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getEmail() {
      return email;
    }

    @JsonSetter("active_status")
    public void setActiveStatus(String active) {
      this.active = "1".equals(active);
    }

    public boolean isValid() {
      return email != null && active;
    }
  }
}
