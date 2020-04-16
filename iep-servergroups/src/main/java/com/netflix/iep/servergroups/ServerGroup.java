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
package com.netflix.iep.servergroups;

import com.netflix.spectator.impl.Preconditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Immutable object representing a server group.
 */
public final class ServerGroup {

  private static final EnumSet<Instance.Status> UNION_STATUSES =
      EnumSet.of(Instance.Status.NOT_REGISTERED, Instance.Status.UP);

  private final String id;
  private final String platform;

  private final String app;
  private final String cluster;
  private final String group;
  private final String stack;
  private final String detail;
  private final String shard1;
  private final String shard2;

  private final int minSize;
  private final int maxSize;
  private final int desiredSize;

  private final List<Instance> instances;

  private ServerGroup(Builder builder) {
    id = builder.platform + "." + builder.group;
    platform = builder.platform;

    com.netflix.spectator.ipc.ServerGroup sg =
        com.netflix.spectator.ipc.ServerGroup.parse(builder.group);
    app = sg.app();
    cluster = sg.cluster();
    group = builder.group;
    stack = sg.stack();
    detail = sg.detail();
    shard1 = sg.shard1();
    shard2 = sg.shard2();

    minSize = builder.minSize;
    maxSize = builder.maxSize;
    desiredSize = builder.desiredSize;

    instances = sort(builder.instances);
  }

  private static List<Instance> sort(List<Instance> instances) {
    List<Instance> tmp = new ArrayList<>(instances);
    tmp.sort(Comparator.comparing(Instance::getNode));
    return Collections.unmodifiableList(tmp);
  }

  /** Return the id for the server group. */
  public String getId() {
    return id;
  }

  /** Return the platform, ec2 or titus, for the server group. */
  public String getPlatform() {
    return platform;
  }

  /**
   * Return the app, for the server group. This is extracted from the group name using the
   * Frigga parsing rules.
   */
  public String getApp() {
    return app;
  }

  /**
   * Return the cluster, for the server group. This is extracted from the group name using the
   * Frigga parsing rules.
   */
  public String getCluster() {
    return cluster;
  }

  /** Return the group name, for the server group. */
  public String getGroup() {
    return group;
  }

  /**
   * Return the stack, for the server group. This is extracted from the group name using the
   * Frigga parsing rules.
   */
  public String getStack() {
    return stack;
  }

  /**
   * Return the detail, for the server group. This is extracted from the group name using the
   * Frigga parsing rules.
   */
  public String getDetail() {
    return detail;
  }

  /**
   * Return the value for the first shard extracted from the detail of the server group based
   * on the ShardingNamingConvention in Frigga.
   */
  public String getShard1() {
    return shard1;
  }

  /**
   * Return the value for the second shard extracted from the detail of the server group based
   * on the ShardingNamingConvention in Frigga.
   */
  public String getShard2() {
    return shard2;
  }

  /** Return the minimum size for the group. */
  public int getMinSize() {
    return minSize;
  }

  /** Return the maximum size for the group. */
  public int getMaxSize() {
    return maxSize;
  }

  /** Return the desired size for the group. */
  public int getDesiredSize() {
    return desiredSize;
  }

  /** Return the set of instances in the group. */
  public List<Instance> getInstances() {
    return instances;
  }

  /**
   * Return a new server group with the attributes merged from this group and the other
   * group. Both groups must have the same id.
   */
  public ServerGroup merge(ServerGroup other) {
    if (!id.equals(other.id)) {
      throw new IllegalArgumentException("merging is only supported for the same group");
    }

    Map<String, Instance> otherInstances = new HashMap<>();
    for (Instance i : other.instances) {
      otherInstances.put(i.getNode(), i);
    }

    List<Instance> merged = new ArrayList<>(instances.size());

    // Instances in this group or both groups
    for (Instance i1 : instances) {
      Instance i2 = otherInstances.remove(i1.getNode());
      if (i2 != null) {
        merged.add(i1.merge(i2));
      } else if (UNION_STATUSES.contains(i1.getStatus())) {
        merged.add(i1);
      }
    }

    // Add instances only in the other group
    List<Instance> onlyInOtherGroup = otherInstances.values()
        .stream()
        .filter(i -> UNION_STATUSES.contains(i.getStatus()))
        .collect(Collectors.toList());
    merged.addAll(onlyInOtherGroup);

    // Sort to ensure that ordering doesn't break comparisons
    merged.sort(Comparator.comparing(Instance::getNode));

    return builder()
        .platform(platform)
        .group(group)
        .minSize(Math.max(minSize, other.minSize))
        .maxSize(Math.max(maxSize, other.maxSize))
        .desiredSize(Math.max(desiredSize, other.desiredSize))
        .addInstances(merged)
        .build();
  }

  @Override public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServerGroup that = (ServerGroup) o;
    return minSize == that.minSize &&
        maxSize == that.maxSize &&
        desiredSize == that.desiredSize &&
        Objects.equals(id, that.id) &&
        Objects.equals(platform, that.platform) &&
        Objects.equals(app, that.app) &&
        Objects.equals(cluster, that.cluster) &&
        Objects.equals(group, that.group) &&
        Objects.equals(stack, that.stack) &&
        Objects.equals(detail, that.detail) &&
        Objects.equals(shard1, that.shard1) &&
        Objects.equals(shard2, that.shard2) &&
        Objects.equals(instances, that.instances);
  }

  @Override public int hashCode() {
    return Objects.hash(
        id, platform,
        app, cluster, group, stack, detail, shard1, shard2,
        minSize, maxSize, desiredSize,
        instances);
  }

  @Override public String toString() {
    return "ServerGroup("
        + "id=" + id + ", "
        + "minSize=" + minSize + ", "
        + "maxSize=" + maxSize + ", "
        + "desiredSize=" + desiredSize + ", "
        + "currentSize=" + instances.size() + ", "
        + "instancesHash=" + instances.hashCode()
        + ")";
  }

  /**
   * Merge both sets of groups.
   *
   * @param gs1
   *     First set of server groups.
   * @param gs2
   *     Second set of server groups.
   * @return
   *     Final set of merged groups.
   */
  public static List<ServerGroup> merge(Collection<ServerGroup> gs1, Collection<ServerGroup> gs2) {

    Map<String, ServerGroup> otherGroups = new HashMap<>();
    for (ServerGroup g : gs2) {
      otherGroups.put(g.id, g);
    }

    List<ServerGroup> merged = new ArrayList<>(gs1.size());

    // Groups in gs1 only or in both lists
    for (ServerGroup g1 : gs1) {
      ServerGroup g2 = otherGroups.remove(g1.id);
      merged.add(g2 == null ? g1 : g1.merge(g2));
    }

    // Add groups only in gs2
    merged.addAll(otherGroups.values());

    return merged;
  }

  /** Return a builder that can be used to create a server group. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for creating a server group. */
  public static class Builder {
    private String platform;
    private String group;
    private int minSize;
    private int maxSize;
    private int desiredSize;
    private List<Instance> instances = new ArrayList<>();

    private Builder() {
    }

    /** Set the platform, either EC2 or Titus, for the server group. This attribute is required. */
    public Builder platform(String value) {
      platform = value;
      return this;
    }

    /** Set the group name, for the server group. This attribute is required. */
    public Builder group(String value) {
      group = value;
      return this;
    }

    /** Set the minimum size of the server group. */
    public Builder minSize(int value) {
      minSize = value;
      return this;
    }

    /** Set the maximum size of the server group. */
    public Builder maxSize(int value) {
      maxSize = value;
      return this;
    }

    /** Set the desired size of the server group. */
    public Builder desiredSize(int value) {
      desiredSize = value;
      return this;
    }

    /** Add an instance to the server group. */
    public Builder addInstance(Instance instance) {
      instances.add(instance);
      return this;
    }

    /** Add a set of instances to the server group. */
    public Builder addInstances(Collection<Instance> instance) {
      instances.addAll(instance);
      return this;
    }

    /** Create a new server group from this builder. */
    public ServerGroup build() {
      Preconditions.checkNotNull(platform, "platform must be set");
      Preconditions.checkNotNull(group, "group must be set");
      return new ServerGroup(this);
    }
  }
}
