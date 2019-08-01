/*
 * Copyright 2014-2019 Netflix, Inc.
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

import com.google.common.annotations.VisibleForTesting;
import com.netflix.iep.leader.api.LeaderElector;
import com.netflix.iep.leader.api.ResourceId;
import com.netflix.iep.service.AbstractService;
import com.netflix.spectator.api.Functions;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Timer;
import com.netflix.spectator.api.patterns.PolledMeter;
import com.netflix.spectator.impl.Scheduler;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@code LeaderService} manages periodic leader election using the provided {@link LeaderElector}.
 */
@Singleton
public class LeaderService extends AbstractService {

  private static final Logger logger = LoggerFactory.getLogger(LeaderService.class);

  private final LeaderElector leaderElector;
  private final Scheduler.Options leaderElectorSchedulerOptions;
  private final Scheduler leaderElectorScheduler;
  // visible for testing
  final Runnable leaderServiceTask = createLeaderServiceTask();

  private final Registry registry;
  private final Id leaderElectionsCounterId;
  private final Timer electorInitializeTimer;
  private final AtomicLong timeSinceLastElection;

  private volatile boolean running = false;

  @Inject
  public LeaderService(LeaderElector leaderElector, Config config, Registry registry) {
    this(
        leaderElector,
        registry,
        new Scheduler(registry, "iep-leader-elector", 1),
        configureSchedulerOptions(config),
        registry.createId("leader.elections"),
        registry.timer("leader.electorInitializeDuration"),
        new AtomicLong()
    );

  }

  public LeaderService(
      LeaderElector leaderElector,
      Registry registry,
      Scheduler leaderElectorScheduler,
      Scheduler.Options leaderElectorSchedulerOptions,
      Id leaderElectionsCounterId,
      Timer electorInitializeTimer,
      AtomicLong timeSinceLastElection) {
    Objects.requireNonNull(leaderElector, "leaderElector");
    Objects.requireNonNull(registry, "registry");
    Objects.requireNonNull(leaderElectorScheduler, "leaderElectorScheduler");
    Objects.requireNonNull(leaderElectorSchedulerOptions, "leaderElectorSchedulerOptions");
    Objects.requireNonNull(leaderElectionsCounterId, "leaderElectionsCounterId");
    Objects.requireNonNull(electorInitializeTimer, "electorInitializeTimer");
    Objects.requireNonNull(timeSinceLastElection, "timeSinceLastElection");

    this.leaderElector = leaderElector;
    this.registry = registry;
    this.leaderElectorScheduler = leaderElectorScheduler;
    this.leaderElectorSchedulerOptions = leaderElectorSchedulerOptions;
    this.leaderElectionsCounterId = leaderElectionsCounterId;
    this.electorInitializeTimer = electorInitializeTimer;
    this.timeSinceLastElection = timeSinceLastElection;
  }

  @VisibleForTesting
  static Scheduler.Options configureSchedulerOptions(Config config) {
    return new Scheduler.Options().withFrequency(
        Scheduler.Policy.FIXED_DELAY,
        config.getDuration("iep.leader.leaderElectorFrequency")
    );
  }

  @Override
  protected void startImpl() {
    logger.info("Starting leader service");

    electorInitializeTimer.record(
        leaderElector::initialize
    );

    running = true;
    startMonitoringTimeSinceLastElection();
    leaderElectorScheduler.schedule(leaderElectorSchedulerOptions, leaderServiceTask);

    logger.info("Leader service started");
  }

  private void startMonitoringTimeSinceLastElection() {
    timeSinceLastElection.set(registry.clock().wallTime());
    PolledMeter.using(registry)
        .withName("leader.timeSinceLastElection")
        .monitorValue(timeSinceLastElection, Functions.AGE);
  }

  private Runnable createLeaderServiceTask() {
    return () -> {
      try {
        if (running) {
          logger.info("Running an election.");
          leaderElector.runElection();
          timeSinceLastElection.set(registry.clock().wallTime());
          registry.counter(leaderElectionsCounterId.withTag("result", "success")).increment();
        } else {
          logger.info("Skipping election run.");
        }
      } catch (Throwable t) {
        final String throwableName = t.getCause() != null ?
            t.getCause().getClass().getSimpleName() : t.getClass().getSimpleName();

        final Id counterIdWithTags =
            leaderElectionsCounterId.withTag("result", "failure").withTag("error", throwableName);
        registry.counter(counterIdWithTags).increment();
        logger.warn("Leader election attempt failed", t);
      }
    };
  }

  @Override
  protected void stopImpl() {
    running = false;

    logger.info("Stopping leader service");

    leaderElectorScheduler.shutdown();
    removeLeadership();

    logger.info("Leader service stopped");
  }

  private void removeLeadership() {
    for (ResourceId resourceId : leaderElector.getResources()) {
      if (!leaderElector.removeLeaderFor(resourceId)) {
        logger.warn(
            "While removing leadership for all resources, could not remove leadership for: {}",
            resourceId
        );
      }
    }
  }
}
