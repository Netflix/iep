/*
 * Copyright 2014-2020 Netflix, Inc.
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
package com.netflix.iep.leader;

import com.netflix.iep.leader.api.LeaderDatabase;
import com.netflix.iep.leader.api.LeaderElector;
import com.netflix.iep.leader.api.LeaderId;
import com.netflix.iep.leader.api.LeaderStatus;
import com.netflix.iep.leader.api.ResourceId;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@code StandardLeaderElector} is a canonical implementation of {@link LeaderElector} and
 * {@link LeaderStatus}. It promotes consistency of metrics and logging, while allowing for
 * different backing {@link LeaderDatabase} implementations. It also provides flexibility in how to
 * react to faults, such as whether to keep or remove the local view of leadership status in the
 * face of errors during {@code LeaderDatabase} operations.
 * <p>
 * This implementation expects the {@code LeaderDatabase} implementation to be thread-safe. If that
 * condition is met, this class is thread-safe.
 * <p>
 * For additional documentation, see the following files in this project:
 * <li>{@code reference.conf} - the full list of configuration options</li>
 * <li>{@code README.conf} - the metrics provided</li>
 */
@Singleton
public class StandardLeaderElector implements LeaderElector, LeaderStatus {

  private static final Logger logger = LoggerFactory.getLogger(StandardLeaderElector.class);

  private final LeaderId leaderId;
  private final LeaderDatabase leaderDatabase;
  private final Config config;
  private final ConcurrentMap<ResourceId, LeaderId> resourceLeaders;
  private final Registry registry;

  private final Id leaderRemovalsCounterId;
  private final Id resourceLeaderGaugeId;
  private final Id resourceWithNoLeaderGaugeId;

  @Inject
  public StandardLeaderElector(LeaderDatabase leaderDatabase, Config config, Registry registry) {
    this(
        LeaderId.create(config),
        leaderDatabase,
        config,
        registry,
        initializeResourceLeaderMap(ResourceId.create(config)),
        registry.createId("leader.removals"),
        registry.createId("leader.resourceLeader"),
        registry.createId("leader.resourceWithNoLeader")
    );
  }

  public StandardLeaderElector(
      LeaderId leaderId,
      LeaderDatabase leaderDatabase,
      Config config,
      Registry registry,
      Map<ResourceId, LeaderId> resourceLeaders,
      Id leaderRemovalsCounterId,
      Id resourceLeaderGaugeId,
      Id resourceWithNoLeaderGaugeId
  ) {
    Objects.requireNonNull(leaderId, "leaderId");
    Objects.requireNonNull(leaderDatabase, "dbTable");
    Objects.requireNonNull(config, "config");
    Objects.requireNonNull(registry, "registry");
    Objects.requireNonNull(resourceLeaders, "resourceLeaders");
    Objects.requireNonNull(leaderRemovalsCounterId, "leaderRemovalsCounterId");
    Objects.requireNonNull(resourceLeaderGaugeId, "resourceLeaderGaugeId");
    Objects.requireNonNull(resourceWithNoLeaderGaugeId, "resourceWithNoLeaderGaugeId");

    this.leaderId = leaderId;
    this.leaderDatabase = leaderDatabase;
    this.config = config;
    this.registry = registry;
    this.resourceLeaders = new ConcurrentHashMap<>(resourceLeaders); // defensive copy

    this.leaderRemovalsCounterId = leaderRemovalsCounterId;
    this.resourceLeaderGaugeId = resourceLeaderGaugeId;
    this.resourceWithNoLeaderGaugeId = resourceWithNoLeaderGaugeId;
  }

  // visible for testing
  static Map<ResourceId, LeaderId> initializeResourceLeaderMap(Collection<ResourceId> ids) {
    return ids.stream().collect(Collectors.toMap(Function.identity(), s -> LeaderId.UNKNOWN));
  }

  @Override
  public boolean addResource(ResourceId resourceId) {
    final boolean addResource = !resourceLeaders.containsKey(resourceId);
    if (addResource) {
      resourceLeaders.put(resourceId, LeaderId.UNKNOWN);
    }

    return addResource;
  }

  @Override
  public LeaderId getLeaderFor(ResourceId resourceId) {
    return resourceLeaders.getOrDefault(resourceId, LeaderId.UNKNOWN);
  }

  @Override
  public Set<ResourceId> getResources() {
    return Collections.unmodifiableSet(resourceLeaders.keySet());
  }

  @Override
  public void initialize() {
    leaderDatabase.initialize();
  }

  @Override
  public boolean removeLeaderFor(ResourceId resourceId) {
    boolean removed = false;
    try {
      removed = leaderDatabase.removeLeadershipFor(resourceId);
      if (removed) {
        resourceLeaders.put(resourceId, LeaderId.NO_LEADER);
        registry.counter(
            leaderRemovalsCounterId
                .withTag("resource", resourceId.getId())
                .withTag("result", "success")
        )
        .increment();
      } else {
        registry.counter(
            leaderRemovalsCounterId
                .withTag("resource", resourceId.getId())
                .withTag("result", "failure")
                .withTag("error", "not_leader")
        )
        .increment();
      }
    } catch (Exception e) {
      final boolean removeLocalLeaderOnError = removeLocalLeaderStatusOnError();
      if (removeLocalLeaderOnError) {
        resourceLeaders.put(resourceId, LeaderId.UNKNOWN);
      }

      final String exceptionName = e.getCause() != null ?
          e.getCause().getClass().getSimpleName() : e.getClass().getSimpleName();
      registry.counter(
          leaderRemovalsCounterId
              .withTag("resource", resourceId.getId())
              .withTag("result", "failure")
              .withTag("error", exceptionName)
      )
      .increment();
      logger.error("Exception during leader removal", e);
    }

    return removed;
  }

  @Override
  public boolean removeResource(ResourceId resourceId) {
    final boolean removed = resourceLeaders.remove(resourceId) != null;
    if (removed) {
      registry.gauge(
          resourceLeaderGaugeId
              .withTag("resource", resourceId.getId())
              .withTag("leader", leaderId.getId()))
          .set(0.0);
    }
    return removed;
  }

  @Override
  public void runElection() {
    resourceLeaders.entrySet().forEach(entry -> {
      final ResourceId resourceId = entry.getKey();
      LeaderId newLeaderId;
      try {
        leaderDatabase.updateLeadershipFor(resourceId);
        newLeaderId = leaderDatabase.getLeaderFor(resourceId);
      } catch (Exception e) {
        logger.error("Exception during leader election", e);
        if (removeLocalLeaderStatusOnError()) {
          newLeaderId = LeaderId.UNKNOWN;
        } else {
          newLeaderId = this.leaderId;
        }
      }
      entry.setValue(newLeaderId);
      final Id idWithResourceTag = resourceLeaderGaugeId.withTag("resource", resourceId.getId());

      final boolean hasLeadership = this.leaderId.equals(newLeaderId);
      // always set this leader's value
      registry
          .gauge(idWithResourceTag.withTag("leader", leaderId.getId()))
          .set(hasLeadership ? 1.0 : 0.0);

      if (!hasLeadership) {
        // If this leader doesn't have leadership, this provides a view of which leader this
        // instance maintains has leadership.
        registry.gauge(idWithResourceTag.withTag("leader", newLeaderId.getId())).set(0.0);
      }

      // NO_LEADER for an extended period suggests something isn't right.
      final boolean noLeader = newLeaderId.equals(LeaderId.NO_LEADER);
      registry
          .gauge(resourceWithNoLeaderGaugeId.withTag("resource", resourceId.getId()))
          .set(noLeader ? 1.0 : 0.0);
    });
  }

  @Override
  public boolean hasLeadership() {
    return resourceLeaders.values().stream().allMatch(leaderId::equals);
  }

  @Override
  public boolean hasLeadershipFor(ResourceId resourceId) {
    return getLeaderFor(resourceId).equals(leaderId);
  }

  private boolean removeLocalLeaderStatusOnError() {
    return config.getBoolean("iep.leader.removeLocalLeaderStatusOnError");
  }
}
