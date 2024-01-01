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

/**
 * The current leadership status.
 */
public interface LeaderStatus {

  /**
   * @return true, if leadership is held for the specified resource
   */
  boolean hasLeadershipFor(ResourceId resource);

  /**
   * @return true, if leadership is held for all resources
   * <p>
   * Note: This API exists primarily for convenience for the common case that only one resource
   * is managed by the {@link LeaderElector}.
   */
  boolean hasLeadership();
}
