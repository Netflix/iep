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
package com.netflix.iep.servergroups;

import com.netflix.iep.service.AbstractService;
import com.netflix.spectator.api.Clock;
import com.netflix.spectator.api.Functions;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.patterns.PolledMeter;
import com.netflix.spectator.impl.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for keeping a local cache of the server groups for an environment and refreshing
 * them in the background.
 */
public class GroupService extends AbstractService {

  private static final Logger LOGGER = LoggerFactory.getLogger(GroupService.class);

  private final Registry registry;
  private final Duration frequency;
  private final Map<String, Loader> loaders;

  private final ConcurrentHashMap<String, AtomicLong> lastUpdateTimes;
  private final Scheduler scheduler;

  private final ConcurrentHashMap<String, List<ServerGroup>> cachedData;
  private volatile List<ServerGroup> merged;

  /**
   * Create a new instance.
   *
   * @param registry
   *     Registry for tracking the ages for refreshing the data from the provided loaders.
   * @param frequency
   *     How frequently to refresh the data from the loaders.
   * @param loaders
   *     Map with a set of loaders to get server group data. The key is the name of the loader
   *     used for logging and metrics.
   */
  public GroupService(
      Registry registry,
      Duration frequency,
      Map<String, Loader> loaders) {
    this.registry = registry;
    this.frequency = frequency;
    this.loaders = new LinkedHashMap<>(loaders);
    this.lastUpdateTimes = new ConcurrentHashMap<>();
    this.scheduler = new Scheduler(registry, "GroupService", loaders.size());
    this.cachedData = new ConcurrentHashMap<>();
  }

  /**
   * Refresh the groups for a given loader once and return true if successful and the groups
   * have been cached.
   */
  private boolean refreshOnce(String loaderName, Loader loader) {
    AtomicLong lastUpdateTime = lastUpdateTimes.get(loaderName);
    if (lastUpdateTime == null) {
      // This can happen on shutdown if a refresh is scheduled after the map has
      // been cleared.
      return false;
    }
    try {
      cachedData.put(loaderName, loader.call());
      lastUpdateTime.set(registry.clock().wallTime());
      merged = null;
      return true;
    } catch (Exception e) {
      LOGGER.warn("failed to refresh groups from {}", loaderName, e);
      return false;
    }
  }

  @Override protected void startImpl() throws Exception {
    final Clock clock = registry.clock();

    for (Map.Entry<String, Loader> entry : loaders.entrySet()) {
      final String loaderName = entry.getKey();
      final Loader loader = entry.getValue();

      // Setup meter to track the age
      AtomicLong lastUpdateTime = PolledMeter.using(registry)
          .withName("iep.groups.dataAge")
          .withTag("id", loaderName)
          .monitorValue(new AtomicLong(clock.wallTime()), Functions.age(clock));
      lastUpdateTimes.put(loaderName, lastUpdateTime);

      // Block startup until we have loaded the data at least once
      final long retryDelay = Math.min(frequency.toMillis(), 1000);
      while (!refreshOnce(loaderName, loader)) {
        LOGGER.warn("waiting for first successful load of {} groups", loaderName);
        Thread.sleep(retryDelay);
      }

      // Schedule for future updates
      Scheduler.Options options = new Scheduler.Options()
          .withInitialDelay(frequency)
          .withFrequency(Scheduler.Policy.FIXED_DELAY, frequency);
      scheduler.schedule(options, () -> refreshOnce(loaderName, loader));
    }
  }

  @Override protected void stopImpl() throws Exception {
    scheduler.shutdown();
    lastUpdateTimes.clear();
  }

  /**
   * Return the current set of server groups merged from all loaders.
   */
  public List<ServerGroup> getGroups() {
    List<ServerGroup> groups = merged;
    if (groups == null) {
      groups = new ArrayList<>();

      // Merge in the order for the keys of the loader
      for (String k : loaders.keySet()) {
        List<ServerGroup> otherGroups = cachedData.get(k);
        if (otherGroups != null && !otherGroups.isEmpty()) {
          groups = ServerGroup.merge(groups, otherGroups);
        }
      }
      groups = Collections.unmodifiableList(groups);

      // Keep a copy so it can be reused if there haven't been any updates
      merged = groups;
    }
    return groups;
  }
}
