/*
 * Copyright 2014-2024 Netflix, Inc.
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
package com.netflix.iep.leader.api;

import java.util.Set;

/**
 * Leader elector provides operations to manage the leader election lifecycle.
 */
public interface LeaderElector {

  /**
   * Add a resourceId for this {@code LeaderElector} to manage. {@link #getLeaderFor(ResourceId)}
   * will return {@link LeaderId#UNKNOWN} for {@code resourceId} until the next time
   * {@link #runElection()} completes.
   *
   * @return true if {@code resourceId} was added, false if not
   */
  boolean addResource(ResourceId resourceId);

  /**
   * @return the ID of the current leader for {@code resourceId}
   */
  LeaderId getLeaderFor(ResourceId resourceId);

  /**
   * @return a point in time, immutable view of the set of resources managed by this
   * {@code LeaderElector}
   */
  Set<ResourceId> getResources();

  /**
   * Perform any required initialization of this {@code LeaderElector} to ensure it is ready for
   * use.
   */
  void initialize();

  /**
   * Remove the current leader for {@code resourceId}.
   * <p>
   * If {@code true} is returned from a call to this API, {@link #getLeaderFor(ResourceId)} must
   * return {@link LeaderId#NO_LEADER} for {@code resourceId} until the next time
   * {@link #runElection()} completes.
   * <p>
   * If {@code false} is returned due to an error, the behavior for
   * {@link #getLeaderFor(ResourceId)} is dictated by the property
   * {@code iep.leader.removeLocalLeaderStatusOnError}.
   *
   * @return true if leadership for {@code resourceId} was removed, false if not
   */
  boolean removeLeaderFor(ResourceId resourceId);

  /**
   * Remove {@code resourceId} from this {@code LeaderElector}'s set of resources.
   *
   * @return true if {@code resourceId} was removed, false if not
   */
  boolean removeResource(ResourceId resourceId);

  /**
   * Run an election.
   */
  void runElection();
}
