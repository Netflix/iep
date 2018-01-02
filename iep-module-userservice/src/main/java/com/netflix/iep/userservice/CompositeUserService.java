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

import com.netflix.iep.service.AbstractService;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Combines a set of other user services into a combined view. It is assumed
 * that there is no overlap in the data sets.
 */
@Singleton
class CompositeUserService extends AbstractService implements UserService {

  private final Set<UserService> services;

  @Inject
  CompositeUserService(Set<UserService> services) {
    this.services = services;
  }

    @Override
    protected void startImpl() throws Exception {
  }

    @Override
    protected void stopImpl() throws Exception {
  }

    @Override
    public Set<String> emailAddresses() {
    Set<String> vs = new TreeSet<>();
    for (UserService service : services) {
      vs.addAll(service.emailAddresses());
    }
    return Collections.unmodifiableSet(vs);
  }

    @Override
    public boolean isValidEmail(String email) {
    return services.stream().anyMatch(s -> s.isValidEmail(email));
  }

    @Override
    public String toValidEmail(String email) {
    for (UserService service : services) {
      String v = service.toValidEmail(email.toLowerCase(Locale.US));
      if (v != null) {
        return v;
      }
    }
    return null;
  }
}
