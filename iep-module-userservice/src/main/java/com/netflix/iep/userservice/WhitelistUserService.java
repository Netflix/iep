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
import com.typesafe.config.Config;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Service based on an explicit list if addresses and mappings provided in the
 * config.
 */
@Singleton
class WhitelistUserService extends AbstractService implements UserService {

  private final Set<String> emails;
  private final Map<String, String> mappings;

  @Inject
  WhitelistUserService(Context context) {
    Config config = context.config().getConfig("whitelist");
    emails = Collections.unmodifiableSet(new TreeSet<>(config.getStringList("emails")));
    mappings = new HashMap<>();
    for (Config c : config.getConfigList("mappings")) {
      mappings.put(c.getString("email"), c.getString("replacement"));
    }
  }

  @Override protected void startImpl() throws Exception {
  }

  @Override protected void stopImpl() throws Exception {
  }

  @Override public Set<String> emailAddresses() {
    return emails;
  }

  @Override public String toValidEmail(String email) {
    return isValidEmail(email)
        ? email
        : mappings.get(UserUtils.baseAddress(email));
  }
}
