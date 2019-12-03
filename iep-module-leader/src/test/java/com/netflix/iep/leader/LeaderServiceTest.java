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

import com.google.common.collect.Sets;
import com.netflix.iep.leader.api.LeaderElector;
import com.netflix.iep.leader.api.LeaderId;
import com.netflix.iep.leader.api.ResourceId;
import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.DefaultRegistry;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Timer;
import com.netflix.spectator.impl.Scheduler;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(JUnit4.class)
public class LeaderServiceTest {

  private LeaderService leaderService;
  private CountingLeaderElector leaderElector;
  private CountDownLatch leaderElectorLatch;
  private final AtomicLong timeSinceLastElection = new AtomicLong();
  private final DefaultRegistry registry = new DefaultRegistry();
  private Id leaderElectionsId = registry.createId("leader.test.elections");
  private Timer electorInitializeTimer = registry.timer("leader.test.electorInitializeDuration");

  @Before
  public void setUp() throws Exception {
    createLeaderService(false);
  }

  @After
  public void tearDown() throws Exception {
    stopLeaderService();
  }

  private void createLeaderService(boolean throwDuringElection) throws Exception {
    leaderElectorLatch = new CountDownLatch(1);
    leaderElector = new CountingLeaderElector(throwDuringElection);
    final Scheduler.Options leaderElectorSchedulerOptions = new Scheduler.Options()
        .withFrequency(Scheduler.Policy.RUN_ONCE, Duration.ZERO);
    final Scheduler leaderElectorScheduler = new Scheduler(registry, "test", 1);
    leaderService = new LeaderService(
        leaderElector,
        registry,
        leaderElectorScheduler,
        leaderElectorSchedulerOptions,
        leaderElectionsId,
        electorInitializeTimer,
        timeSinceLastElection);
    leaderService.start();
    leaderElectorLatch.await(6, TimeUnit.SECONDS);
  }

  private void stopLeaderService() throws Exception {
    leaderService.stop();
    registry.reset();
  }

  @Test
  public void runElectionIsCalled() throws Exception {
    assertThat(leaderElector.runElectionCount).isEqualTo(1);
  }

  @Test
  public void electionsCounterIsIncrementedOnSuccess() throws Exception {
    // The sleep() is not ideal, but the flow of control issue here causes a timing issue that's out
    // of the test's ability to manage
    Thread.sleep(10);

    final Counter counter = registry.counter(leaderElectionsId.withTag("result", "success"));
    assertThat(counter.count()).isEqualTo(1);
  }

  @Test
  public void electionsCounterIsIncrementedOnException() throws Exception {
    stopLeaderService();
    createLeaderService(true);

    // The sleep() is not ideal, but the flow of control issue here causes a timing issue that's out
    // of the test's ability to manage
    Thread.sleep(10);

    final Id idWithTags =
        leaderElectionsId.withTags("result", "failure").withTag("error", "RuntimeException");
    final Counter counter = registry.counter(idWithTags);
    assertThat(counter.count()).isEqualTo(1);
  }

  @Test
  public void initializeIsCalled() throws Exception {
    assertThat(leaderElector.initializationCount).isEqualTo(1);
  }

  @Test
  public void removeLeadersIsCalled() throws Exception {
    leaderService.stop();
    assertThat(leaderElector.removeLeaderCount).isEqualTo(1);
  }

  @Test
  public void configureSchedulerOptionsUsesConfigForFrequency() {
    // have to test indirectly, since Scheduler.Options does not allow access to the configured
    // options and there is a bias against using mocking frameworks in `iep`
    final String configString = "iep.leader.bad.path = 10s";
    final Config config = ConfigFactory.parseString(configString);

    assertThatThrownBy(() -> LeaderService.configureSchedulerOptions(config))
        .hasMessageContaining("iep.leader.leaderElectorFrequency");
  }

  @Test
  public void electorInitializeTimerIsSet() {
    assertThat(electorInitializeTimer.totalTime()).isGreaterThan(0);
  }

  @Test
  public void timeSinceLastElectionIsSet() throws Exception {
    long firstElectionTime = this.timeSinceLastElection.get();
    Thread.sleep(300);
    leaderService.leaderServiceTask.run();
    final long lastElectionTime = timeSinceLastElection.get();
    final Duration electionInterval = Duration.ofMillis(lastElectionTime - firstElectionTime);

    assertThat(electionInterval).isGreaterThan(Duration.ofMillis(295));
  }

  private class CountingLeaderElector implements LeaderElector {
    boolean throwDuringElection;
    int initializationCount = 0;
    int runElectionCount = 0;
    int removeLeaderCount = 0;

    private final HashSet<ResourceId> resourceIds = Sets.newHashSet(new ResourceId("test"));

    CountingLeaderElector(boolean throwDuringElection) {
      this.throwDuringElection = throwDuringElection;
    }

    @Override
    public boolean addResource(ResourceId resourceId) {
      return false;
    }

    @Override
    public LeaderId getLeaderFor(ResourceId resourceId) {
      return LeaderId.NO_LEADER;
    }

    @Override
    public Set<ResourceId> getResources() {
      return resourceIds;
    }

    @Override
    public void initialize() {
      ++initializationCount;
    }

    @Override
    public boolean removeLeaderFor(ResourceId resourceId) {
      ++removeLeaderCount;
      return true;
    }

    @Override
    public boolean removeResource(ResourceId resourceId) {
      return false;
    }

    @Override
    public void runElection() {
      ++runElectionCount;
      leaderElectorLatch.countDown();
      if (throwDuringElection) {
        throw new RuntimeException("test exception configured for runElection");
      }
    }
  }
}