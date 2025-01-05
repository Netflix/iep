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
package com.netflix.iep.leader.api;

/**
 * {@code LeaderDatabase} provides operations for storage, update, and access to the set of managed
 * resources and the current leader of each.
 */
public interface LeaderDatabase {

  /**
   * Perform any required initialization for this {@code LeaderDatabase}.
   */
  void initialize();

  /**
   * Retrieve the current leader for {@code resourceId}.
   */
  LeaderId getLeaderFor(ResourceId resourceId);

  /**
   * For {@code resourceId}, conditionally insert or update its record with the configured
   * {@code iep.leader.leaderId} if the criteria for assuming leadership are met.
   */
  boolean updateLeadershipFor(ResourceId resourceId);

  /**
   * For {@code resourceId}, update its record to {@link LeaderId#NO_LEADER} if its current leader
   * is the configured {@code iep.leader.leaderId} of this instance.
   */
  boolean removeLeadershipFor(ResourceId resourceId);

}
