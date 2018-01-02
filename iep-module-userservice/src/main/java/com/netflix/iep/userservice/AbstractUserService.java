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
import com.netflix.spectator.api.Clock;
import com.netflix.spectator.api.Functions;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.sandbox.HttpResponse;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.stream.Collectors.toSet;

/**
 * Base class for user services that fetch a JSON payload from an HTTP endpoint.
 */
abstract class AbstractUserService extends AbstractService implements UserService {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final String name = getClass().getSimpleName();

  protected final Context context;
  protected final String uri;
  protected final boolean enabled;

  private final AtomicLong lastUpdateTime;
  protected final AtomicReference<Set<String>> emails =
    new AtomicReference<>(Collections.emptySet());

  AbstractUserService(Context context, String service) {
    this.context = context;
    Config config = context.config().getConfig(service);
    this.uri = config.getString("uri");
    this.enabled = config.getBoolean("enabled");

    Registry registry = context.registry();
    Clock clock = registry.clock();
    Id cacheAge = registry.createId("iep.users.cacheAge", "id", name);
    lastUpdateTime = enabled
        ? registry.gauge(cacheAge, new AtomicLong(clock.wallTime()), Functions.age(clock))
        : new AtomicLong(0L);
  }

  protected abstract Set<String> parseResponse(byte[] data) throws IOException;

  @Override public Set<String> emailAddresses() {
    return emails.get();
  }

  @Override
  protected void startImpl() throws Exception {
    if (enabled) {
      // Block until we are able to get list at least once
      while (!refresh()) {
        Thread.sleep(context.frequency());
      }

      // Startup background task to regularly refresh
      context.executor().scheduleWithFixedDelay(this::refresh,
          0L, context.frequency(), TimeUnit.MILLISECONDS);
    } else {
      logger.debug("service not enabled");
    }
  }

  @Override
  protected void stopImpl() throws Exception {
  }

  private boolean refresh() {
    try {
      refreshData();
      lastUpdateTime.set(context.registry().clock().wallTime());
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      logger.warn("failed to refresh users list", e);
      return false;
    }
  }

  private void refreshData() throws IOException {
    HttpResponse res = context.get(name, uri);
    if (res.status() == 200) {
      emails.set(parseResponse(res.entity()).stream()
          .map(email -> email.toLowerCase(Locale.US))
          .collect(toSet()));
    } else {
      throw new IOException("request to " + uri + " failed with status " + res.status());
    }
  }
}
