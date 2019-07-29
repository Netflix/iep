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
import com.netflix.spectator.api.Registry;
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

  @Before
  public void setUp() throws Exception {
    leaderElectorLatch = new CountDownLatch(1);
    leaderElector = new CountingLeaderElector();
    final Registry registry = new DefaultRegistry();
    final Scheduler.Options leaderElectorSchedulerOptions = new Scheduler.Options()
        .withFrequency(Scheduler.Policy.RUN_ONCE, Duration.ZERO);
    final Scheduler leaderElectorScheduler = new Scheduler(registry, "test", 1);
    final Counter leaderElectionFailureCounter = registry.counter("leaderElectionFailureCounter");
    leaderService = new LeaderService(
        leaderElector,
        registry,
        leaderElectorScheduler,
        leaderElectorSchedulerOptions,
        leaderElectionFailureCounter,
        timeSinceLastElection);

    leaderService.start();
  }

  @After
  public void tearDown() throws Exception {
    leaderService.stop();
  }

  @Test
  public void runElectionIsCalled() throws Exception {
    leaderElectorLatch.await(1, TimeUnit.SECONDS);
    assertThat(leaderElector.runElectionCount).isEqualTo(1);
  }

  @Test
  public void initializeIsCalled() throws Exception {
    leaderElectorLatch.await(1, TimeUnit.SECONDS);
    assertThat(leaderElector.initializationCount).isEqualTo(1);
  }

  @Test
  public void removeLeadersIsCalled() throws Exception {
    leaderService.stop();
    leaderElectorLatch.await(1, TimeUnit.SECONDS);
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
  public void timeSinceLastElectionIsSet() throws Exception {
    leaderElectorLatch.await(1, TimeUnit.SECONDS);
    long firstElectionTime = this.timeSinceLastElection.get();
    Thread.sleep(300);
    leaderService.leaderServiceTask.run();
    final long lastElectionTime = timeSinceLastElection.get();
    final Duration electionInterval = Duration.ofMillis(lastElectionTime - firstElectionTime);

    System.out.println(electionInterval);
    assertThat(electionInterval).isGreaterThan(Duration.ofMillis(295));
  }

  private class CountingLeaderElector implements LeaderElector {
    int initializationCount = 0;
    int runElectionCount = 0;
    int removeLeaderCount = 0;
    private final HashSet<ResourceId> resourceIds = Sets.newHashSet(new ResourceId("test"));

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
    }
  }
}