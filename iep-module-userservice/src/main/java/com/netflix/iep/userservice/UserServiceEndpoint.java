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

import com.netflix.iep.admin.HttpEndpoint;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provide visibility into current state of the user service.
 *
 * <pre>
 * /
 *     Returns result of {@link UserService#emailAddresses()}.
 * /foo@example.com
 *     Returns result of a check on the address.
 * </pre>
 */
public class UserServiceEndpoint implements HttpEndpoint {

  private final UserService service;

  @Inject
  public UserServiceEndpoint(UserService service) {
    this.service = service;
  }

  @Override public Object get() {
    return service.emailAddresses();
  }

  @Override public Object get(String path) {
    Object obj;
    String[] parts = path.split("/");
    if (parts.length == 1) {
      Map<String, Object> response = new LinkedHashMap<>();
      response.put("isValidEmail", service.isValidEmail(parts[0]));
      response.put("toValidEmail", service.toValidEmail(parts[0]));
      obj = response;
    } else {
      obj = null;
    }
    return obj;
  }
}
