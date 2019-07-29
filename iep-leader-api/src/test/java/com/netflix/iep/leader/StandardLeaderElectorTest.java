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

import com.netflix.iep.leader.api.LeaderDatabase;
import com.netflix.iep.leader.api.LeaderElector;
import com.netflix.iep.leader.api.LeaderId;
import com.netflix.iep.leader.api.ResourceId;
import com.netflix.spectator.api.DefaultRegistry;
import com.netflix.spectator.api.Gauge;
import com.netflix.spectator.api.Id;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

@RunWith(JUnit4.class)
public class StandardLeaderElectorTest {
  private final String defaultConfigString = createConfigString(false);
  private final Config defaultConfig = ConfigFactory.parseString(defaultConfigString);
  private final DefaultRegistry defaultRegistry = new DefaultRegistry();
  private final Id resourceLeaderGaugeId = defaultRegistry.createId("leader.test.resourceLeader");
  private final Id noLeaderGaugeId = defaultRegistry.createId("leader.test.resourceWithNoLeader");
  private final Id removalFailureMetricId = defaultRegistry.createId("leader.test.removalFailure");
  private final LeaderId leaderId = LeaderId.create("test-leader");
  private final LeaderId otherLeaderId = LeaderId.create("test-other-leader");
  private TestableLeaderDatabase testableLeaderDatabase;
  private StandardLeaderElector leaderElector;

  private String createConfigString(boolean removeLocalLeaderOnError) {
    return String.format(
        "%s = %b%n",
        "iep.leader.removeLocalLeaderStatusOnError",
        removeLocalLeaderOnError);
  }

  @Before
  public void setup() {
    testableLeaderDatabase = new TestableLeaderDatabase(leaderId, otherLeaderId);
    leaderElector = createLeaderElector(defaultConfig);
  }

  @After
  public void tearDown() {
    defaultRegistry.reset();
  }

  private StandardLeaderElector createLeaderElector(Config config) {
    return new StandardLeaderElector(
        leaderId,
        testableLeaderDatabase,
        config,
        defaultRegistry,
        new HashMap<>(),
        removalFailureMetricId,
        resourceLeaderGaugeId,
        noLeaderGaugeId
    );
  }

  @Test
  public void initializeResourceLeaderMapHandlesEmptyCollection() {
    final Map<ResourceId, LeaderId> resourceIdLeaderIdMap =
        StandardLeaderElector.initializeResourceLeaderMap(Collections.emptyList());
    assertThat(resourceIdLeaderIdMap).isEmpty();
  }

  @Test
  public void initializeResourceLeaderMapHandlesOneResource() {
    final ResourceId id1 = new ResourceId("test-resource-one");
    final Map<ResourceId, LeaderId> resourceIdLeaderIdMap =
        StandardLeaderElector.initializeResourceLeaderMap(Lists.list(id1));

    assertThat(resourceIdLeaderIdMap).hasSize(1);
    assertThat(resourceIdLeaderIdMap).containsEntry(id1, LeaderId.UNKNOWN);
  }

  @Test
  public void initializeResourceLeaderMapHandlesMultipleResources() {
    final ResourceId id1 = new ResourceId("test-resource-one");
    final ResourceId id2 = new ResourceId("test-resource-two");
    final ResourceId id3 = new ResourceId("test-resource-three");
    final Map<ResourceId, LeaderId> resourceIdLeaderIdMap =
        StandardLeaderElector.initializeResourceLeaderMap(Lists.list(id1, id2, id3));

    assertThat(resourceIdLeaderIdMap)
        .extractingFromEntries(Map.Entry::getKey, Map.Entry::getValue)
        .containsExactlyInAnyOrder(
            tuple(id1, LeaderId.UNKNOWN),
            tuple(id2, LeaderId.UNKNOWN),
            tuple(id3, LeaderId.UNKNOWN)
        );
  }

  @Test
  public void resourceIsAdded() {
    final ResourceId resourceId = new ResourceId("resourceIsAdded");
    assertThat(leaderElector.addResource(resourceId)).isTrue();
    assertThat(leaderElector.getResources()).containsExactly(resourceId);
  }

  @Test
  public void multipleResourcesAreAdded() {
    final ResourceId id1 = new ResourceId("test-resource-one");
    final ResourceId id2 = new ResourceId("test-resource-two");
    assertThat(leaderElector.addResource(id1)).isTrue();
    assertThat(leaderElector.addResource(id2)).isTrue();
    assertThat(leaderElector.getResources()).containsExactly(id1, id2);
  }

  @Test
  public void resourceIsRemoved() {
    final ResourceId id1 = new ResourceId("test-resource-one");
    final ResourceId id2 = new ResourceId("test-resource-two");
    leaderElector.addResource(id1);
    leaderElector.addResource(id2);
    assertThat(leaderElector.getResources()).containsExactly(id1, id2);
    assertThat(leaderElector.removeResource(id1)).isTrue();
    assertThat(leaderElector.getResources()).doesNotContain(id1);
    assertThat(leaderElector.getResources()).contains(id2);
  }

  @Test
  public void newResourceHasUnknownLeader() {
    final ResourceId resourceId = new ResourceId("noLeaderResource");
    leaderElector.addResource(resourceId);
    assertThat(leaderElector.getLeaderFor(resourceId)).isEqualTo(LeaderId.UNKNOWN);
  }

  @Test
  public void getLeaderReturnsUnknownLeaderForUnknownResource() {
    final ResourceId resourceId = new ResourceId("UnknownResource");
    leaderElector.addResource(resourceId);
    assertThat(leaderElector.getLeaderFor(resourceId)).isEqualTo(LeaderId.UNKNOWN);
  }

  @Test
  public void getResourcesReturnsSetThatIsNotModifiable() {
    final ResourceId resourceId = new ResourceId("test-resource");
    leaderElector.addResource(resourceId);
    assertThatThrownBy(() ->
        leaderElector.getResources().add(new ResourceId("another-test-resource"))
    )
    .isExactlyInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void leaderElectorInitializesDatabase() {
    leaderElector.initialize();
    assertThat(testableLeaderDatabase.initialized).isTrue();
  }

  @Test
  public void runElectionChoosesThisLeaderOnWin() {
    final ResourceId id1 = new ResourceId("test-resource-one");
    final ResourceId id2 = new ResourceId("test-resource-two");

    leaderElector.addResource(id1);
    leaderElector.addResource(id2);

    leaderElector.runElection();

    assertThat(leaderElector.getLeaderFor(id1)).isEqualTo(leaderId);
    assertThat(leaderElector.getLeaderFor(id2)).isEqualTo(leaderId);
  }

  @Test
  public void runElectionChoosesOtherLeaderOnNoWin() {
    final ResourceId id1 = new ResourceId("test-resource-one");
    final ResourceId id2 = new ResourceId("test-resource-two");

    leaderElector.addResource(id1);
    leaderElector.addResource(id2);

    testableLeaderDatabase.winLeadershipInUpdate = false;
    leaderElector.runElection();

    assertThat(leaderElector.getLeaderFor(id1)).isEqualTo(otherLeaderId);
    assertThat(leaderElector.getLeaderFor(id2)).isEqualTo(otherLeaderId);
  }

  @Test
  public void runElectionExceptionKeepsExistingLeaderWhenConfiguredTo() {
    final ResourceId id1 = new ResourceId("test-resource-one");

    leaderElector.addResource(id1);

    leaderElector.runElection();
    assertThat(leaderElector.getLeaderFor(id1)).isEqualTo(leaderId);

    testableLeaderDatabase.winLeadershipInUpdate = false;
    testableLeaderDatabase.throwExceptionInUpdate = true;
    leaderElector.runElection();
    assertThat(leaderElector.getLeaderFor(id1)).isEqualTo(leaderId);
  }

  @Test
  public void runElectionExceptionRemovesLocalLeaderWhenConfiguredTo() {
    final ResourceId id1 = new ResourceId("test-resource-one");
    final Config config = ConfigFactory.parseString(createConfigString(true));
    final LeaderElector leaderElector = createLeaderElector(config);

    leaderElector.addResource(id1);

    leaderElector.runElection();
    assertThat(leaderElector.getLeaderFor(id1)).isEqualTo(leaderId);

    testableLeaderDatabase.winLeadershipInUpdate = false;
    testableLeaderDatabase.throwExceptionInUpdate = true;
    leaderElector.runElection();
    assertThat(leaderElector.getLeaderFor(id1)).isEqualTo(LeaderId.UNKNOWN);
  }

  @Test
  public void runElectionSetsGaugeWithLeader() {
    final ResourceId id1 = new ResourceId("test-resource-one");
    leaderElector.addResource(id1);

    final Id idWithResourceTag = resourceLeaderGaugeId.withTag("resource", id1.getId());
    final Id leaderMetricId = idWithResourceTag.withTag("leader", leaderId.getId());
    final Id otherLeaderMetricId = idWithResourceTag.withTag("leader", otherLeaderId.getId());

    final Gauge leaderGauge = defaultRegistry.gauge(leaderMetricId);
    final Gauge otherLeaderGauge = defaultRegistry.gauge(otherLeaderMetricId);
    final Gauge noLeaderGauge =
        defaultRegistry.gauge(noLeaderGaugeId.withTag("resource", id1.getId()));

    testableLeaderDatabase.winLeadershipInUpdate = true;
    leaderElector.runElection();

    assertThat(leaderGauge.value()).isEqualTo(1.0);
    assertThat(otherLeaderGauge.value()).isNaN();
    assertThat(noLeaderGauge.value()).isEqualTo(0.0);

    testableLeaderDatabase.winLeadershipInUpdate = false;
    leaderElector.runElection();

    assertThat(leaderGauge.value()).isEqualTo(0.0);
    // Tag is set, but value is meant to be 0.0 to indicate the reporting instance is not the leader
    assertThat(otherLeaderGauge.value()).isEqualTo(0.0);
    assertThat(noLeaderGauge.value()).isEqualTo(0.0);

    testableLeaderDatabase.winLeadershipInUpdate = true;
    leaderElector.runElection();

    assertThat(leaderGauge.value()).isEqualTo(1.0);
    assertThat(otherLeaderGauge.value()).isEqualTo(0.0);
    assertThat(noLeaderGauge.value()).isEqualTo(0.0);
  }

  @Test
  public void runElectionSetsResourceLeaderGaugeToUnknownWhenRemoveLocalLeaderIsConfigured() {
    final ResourceId id1 = new ResourceId("test-resource-one");
    final Config config = ConfigFactory.parseString(createConfigString(true));
    final LeaderElector leaderElector = createLeaderElector(config);

    leaderElector.addResource(id1);

    leaderElector.runElection();
    assertThat(leaderElector.getLeaderFor(id1)).isEqualTo(leaderId);

    testableLeaderDatabase.winLeadershipInUpdate = false;
    testableLeaderDatabase.throwExceptionInUpdate = true;
    leaderElector.runElection();

    final Gauge leaderGauge = defaultRegistry.gauge(
        resourceLeaderGaugeId
            .withTag("resource", id1.getId())
            .withTag("leader", LeaderId.UNKNOWN.getId())
    );
    assertThat(leaderGauge.value()).isEqualTo(0.0);

    final Gauge noLeaderGauge =
        defaultRegistry.gauge(noLeaderGaugeId.withTag("resource", id1.getId()));
    // only set to 1.0 if the leader is specifically set to NO_LEADER
    assertThat(noLeaderGauge.value()).isEqualTo(0.0);
  }

  @Test
  public void runElectionSetsResourceLeaderGaugeToNoLeaderWhenTheResourceHasNoLeader() {
    final ResourceId id1 = new ResourceId("test-resource-one");

    leaderElector.addResource(id1);

    final Gauge noLeaderGauge =
        defaultRegistry.gauge(noLeaderGaugeId.withTag("resource", id1.getId()));

    testableLeaderDatabase.winLeadershipInUpdate = true;
    leaderElector.runElection();
    assertThat(leaderElector.getLeaderFor(id1)).isEqualTo(leaderId);
    assertThat(noLeaderGauge.value()).isEqualTo(0.0);

    testableLeaderDatabase.winLeadershipInUpdate = false;
    leaderElector.removeLeaderFor(id1);
    leaderElector.runElection();
    assertThat(leaderElector.getLeaderFor(id1)).isEqualTo(LeaderId.NO_LEADER);

    assertThat(noLeaderGauge.value()).isEqualTo(1.0);

    testableLeaderDatabase.winLeadershipInUpdate = true;
    leaderElector.runElection();
    assertThat(noLeaderGauge.value()).isEqualTo(0.0);
  }

  @Test
  public void runElectionUpdatesTheDatabase() {
    leaderElector.addResource(new ResourceId("test-resource-one"));
    leaderElector.runElection();
    assertThat(testableLeaderDatabase.databaseHitCount).isEqualTo(2);
  }

  @Test
  public void removeLeaderUpdatesTheDatabase() {
    leaderElector.removeLeaderFor(new ResourceId("test-resource-one"));
    assertThat(testableLeaderDatabase.databaseHitCount).isEqualTo(1);
  }

  @Test
  public void hasLeadershipDoesNotHitTheDatabase() {
    leaderElector.addResource(new ResourceId("test-resource-one"));
    leaderElector.runElection();
    int databaseHits = testableLeaderDatabase.databaseHitCount;
    leaderElector.hasLeadership();

    assertThat(testableLeaderDatabase.databaseHitCount).isEqualTo(databaseHits);
  }

  @Test
  public void hasLeadershipForDoesNotHitTheDatabase() {
    final ResourceId resourceId = new ResourceId("test-resource-one");
    leaderElector.addResource(resourceId);
    leaderElector.runElection();
    int databaseHits = testableLeaderDatabase.databaseHitCount;
    leaderElector.hasLeadershipFor(resourceId);

    assertThat(testableLeaderDatabase.databaseHitCount).isEqualTo(databaseHits);
  }

  @Test
  public void addResourceDoesNotHitTheDatabase() {
    leaderElector.addResource(new ResourceId("test-resource-one"));
    assertThat(testableLeaderDatabase.databaseHitCount).isEqualTo(0);
  }

  @Test
  public void removeResourceDoesNotHitTheDatabase() {
    leaderElector.addResource(new ResourceId("test-resource-one"));
    leaderElector.runElection();
    int databaseHits = testableLeaderDatabase.databaseHitCount;
    leaderElector.removeResource(new ResourceId("test-resource-one"));
    assertThat(testableLeaderDatabase.databaseHitCount).isEqualTo(databaseHits);
  }

  private static class TestableLeaderDatabase implements LeaderDatabase {

    boolean winLeadershipInUpdate = true;
    boolean throwExceptionInUpdate = false;
    boolean allowRemoveLeadership = true;

    boolean initialized = false;
    int databaseHitCount = 0;

    private final LeaderId leaderId;
    private final LeaderId otherLeaderId;
    private final Map<ResourceId, LeaderId> resourceLeaders = new HashMap<>();

    TestableLeaderDatabase(LeaderId leaderId, LeaderId otherLeaderId) {
      this.leaderId = leaderId;
      this.otherLeaderId = otherLeaderId;
    }

    @Override
    public void initialize() {
      ++databaseHitCount;
      initialized = true;
    }

    @Override
    public LeaderId getLeaderFor(ResourceId resourceId) {
      ++databaseHitCount;
      return resourceLeaders.getOrDefault(resourceId, LeaderId.UNKNOWN);
    }

    @Override
    public boolean updateLeadershipFor(ResourceId resourceId) {
      ++databaseHitCount;
      if (throwExceptionInUpdate) {
        throw new RuntimeException("test exception configured for updateLeadershipFor");
      }

      if (winLeadershipInUpdate) {
        resourceLeaders.put(resourceId, leaderId);
      } else if(!resourceLeaders.getOrDefault(resourceId, LeaderId.UNKNOWN)
          .equals(LeaderId.NO_LEADER)) {
        resourceLeaders.put(resourceId, otherLeaderId);
      }
      return winLeadershipInUpdate;
    }

    @Override
    public boolean removeLeadershipFor(ResourceId resourceId) {
      ++databaseHitCount;
      if (allowRemoveLeadership &&
          resourceLeaders.getOrDefault(resourceId, LeaderId.UNKNOWN).equals(leaderId)) {
        resourceLeaders.put(resourceId, LeaderId.NO_LEADER);
      }
      return allowRemoveLeadership;
    }
  }
}