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
package com.netflix.iep.leader.api;

import com.typesafe.config.Config;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * An identifier for a resource that needs a leader.
 */
public final class ResourceId {

  private final String id;

  public static final String RESOURCE_IDS_CONFIG_PATH = "iep.leader.resourceIds";

  /**
   * Creates a collection of {@code ResourceId}s using the values provided in {@code config} for
   * {@link #RESOURCE_IDS_CONFIG_PATH}.
   */
  public static Collection<ResourceId> create(Config config) {
    final Collection<ResourceId> resourceIds;
    if (config.hasPath(RESOURCE_IDS_CONFIG_PATH)) {
      resourceIds = config.getStringList(RESOURCE_IDS_CONFIG_PATH)
          .stream()
          .map(ResourceId::new)
          .collect(Collectors.toList());
    } else {
      resourceIds = Collections.emptyList();
    }

    return resourceIds;
  }

  /**
   * Constructs a {@code ResourceId}.
   *
   * @param id an ID that is unique within the scope of visibility of the entities that can lead
   *           the resource
   * @throws NullPointerException if {@code id} is null
   * @throws IllegalArgumentException if {@code id} is empty
   */
  public ResourceId(String id) {
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
    return "ResourceId{" +
        "id='" + id + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ResourceId that = (ResourceId) o;
    return id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
