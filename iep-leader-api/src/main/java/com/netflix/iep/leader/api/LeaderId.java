/*
 * Copyright 2014-2022 Netflix, Inc.
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

import com.typesafe.config.Config;

import java.util.Objects;

/**
 * An identifier for a leader that may lead a resource.
 */
public final class LeaderId {

  private static final String LEADER_ID_CONFIG_PATH = "iep.leader.leaderId";

  private final String id;

  /**
   * Constant for when there is currently no elected leader.
   */
  public static final LeaderId NO_LEADER = new LeaderId("NO_LEADER");

  /**
   * Constant for when the is currently elected leader is not known.
   */
  public static final LeaderId UNKNOWN = new LeaderId("UNKNOWN");

  /**
   * Constructs a {@code LeaderId} using value provided in {@code config} for
   * {@link #LEADER_ID_CONFIG_PATH}.
   */
  public static LeaderId create(Config config) {
    try {
      return new LeaderId(config.getString(LEADER_ID_CONFIG_PATH));
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid '" + LEADER_ID_CONFIG_PATH + "'", e);
    }
  }

  /**
   * Returns a {@code LeaderId} for {@code id}, creating one if necessary.
   *
   * @param id an ID that is unique among the entities that can lead the resource
   * @throws NullPointerException if {@code id} is null
   * @throws IllegalArgumentException if {@code id} is empty
   */
  public static LeaderId create(String id) {
    final LeaderId leaderId;
    switch (id) {
      case "NO_LEADER":
        leaderId = LeaderId.NO_LEADER;
        break;
      case "UNKNOWN":
        leaderId = LeaderId.UNKNOWN;
        break;
      default:
        leaderId = new LeaderId(id);
    }

    return leaderId;
  }

  private LeaderId(String id) {
    Objects.requireNonNull(id, "id");
    if (id.isEmpty()) {
      throw new IllegalArgumentException("id is empty");
    }

    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    return "LeaderId{" +
        "id='" + id + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LeaderId leaderId = (LeaderId) o;
    return id.equals(leaderId.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
