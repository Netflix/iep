/*
 * Copyright 2014-2023 Netflix, Inc.
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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.netflix.iep.service.AbstractService;
import com.netflix.spectator.ipc.http.HttpResponse;
import com.typesafe.config.Config;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Calls one or more remote services via HTTP to check if an email is valid. The only
 * supported call is {@link #isValidEmail(String)}, the others just return an empty set.
 * Email checks will get cached to reduce load and improve performance for repeated checks.
 */
@Singleton
public class HttpUserService extends AbstractService implements UserService {

  private final Context context;
  private final boolean enabled;
  private final List<? extends Config> uris;

  private final LoadingCache<String, Boolean> cache;

  @Inject
  HttpUserService(Context context) {
    this(context, "http");
  }

  HttpUserService(Context context, String name) {
    this.context = context;

    Config config = context.config().getConfig(name);
    this.enabled = config.getBoolean("enabled");
    this.uris = config.getConfigList("uris");

    this.cache = Caffeine.newBuilder()
        .expireAfterWrite(config.getDuration("cache-ttl"))
        .maximumSize(10_000)
        .build(this::isValidEmailImpl);
  }

  @Override protected void startImpl() throws Exception {
  }

  @Override protected void stopImpl() throws Exception {
  }

  @Override public Set<String> emailAddresses() {
    return Collections.emptySet();
  }

  @Override
  public boolean isValidEmail(String email) {
    String sanitizedEmail = UserUtils.baseAddress(email.toLowerCase(Locale.US));
    return enabled && checkCache(sanitizedEmail);
  }

  private boolean checkCache(String email) {
    Boolean result = cache.get(email);
    return result != null && result;
  }

  private boolean isValidEmailImpl(String email) {
    return uris.stream().anyMatch(c -> checkRemoteService(c, email));
  }

  private String extractEndpoint(String uri) {
    // The path is from a static config and is used before substitution of the email. So it
    // is guaranteed to be bounded.
    String fixedUri = uri.replace('{', '_').replace('}', '_');
    return URI.create(fixedUri).getPath();
  }

  private boolean checkRemoteService(Config config, String email) {
    final String configUri = config.getString("uri");
    final String endpoint = extractEndpoint(configUri);
    final String uri = configUri.replace("{email}", email);
    final boolean useSslFactory = config.getBoolean("use-ssl-factory");
    final boolean validOnFailure = config.getBoolean("valid-on-failure");
    try {
      HttpResponse response = context.get(endpoint, uri, useSslFactory);
      int status = response.status();
      return status == 200 || (status >= 500 && validOnFailure);
    } catch (Exception e) {
      return validOnFailure;
    }
  }
}
