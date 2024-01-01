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
package com.netflix.iep.servergroups;

import com.netflix.spectator.impl.Preconditions;

import java.util.Objects;

/**
 * Immutable object representing an instance in a server group.
 */
public final class Instance {

  private final String node;
  private final String privateIpAddress;

  private final String vpcId;
  private final String subnetId;

  private final String ami;
  private final String vmtype;
  private final String zone;

  private final Status status;

  private Instance(Builder builder) {
    node = builder.node;
    privateIpAddress = builder.privateIpAddress;
    vpcId = builder.vpcId;
    subnetId = builder.subnetId;
    ami = builder.ami;
    vmtype = builder.vmtype;
    zone = builder.zone;
    status = builder.status;
  }

  /** Return the id for the instance. */
  public String getNode() {
    return node;
  }

  /** Return the IP address for the instance. */
  public String getPrivateIpAddress() {
    return privateIpAddress;
  }

  /** Return the VPC ID for the instance. */
  public String getVpcId() {
    return vpcId;
  }

  /** Return the subnet ID for the instance. */
  public String getSubnetId() {
    return subnetId;
  }

  /** Return the image ID for the instance. */
  public String getAmi() {
    return ami;
  }

  /** Return the vmtype for the instance. */
  public String getVmtype() {
    return vmtype;
  }

  /** Return the availability zone for the instance. */
  public String getZone() {
    return zone;
  }

  /** Return the status in Eureka for the instance. */
  public Status getStatus() {
    return status;
  }

  private String orElse(String v1, String v2) {
    return v1 == null ? v2 : v1;
  }

  /**
   * Return a new instance with the attributes merged from this instance and the other
   * instance. Both instances must have the same instance id.
   */
  public Instance merge(Instance other) {
    if (!node.equals(other.node)) {
      throw new IllegalArgumentException("merging is only supported for the same instance");
    }
    return builder()
        .node(node)
        .privateIpAddress(orElse(privateIpAddress, other.privateIpAddress))
        .vpcId(orElse(vpcId, other.vpcId))
        .subnetId(orElse(subnetId, other.subnetId))
        .ami(orElse(ami, other.ami))
        .vmtype(orElse(vmtype, other.vmtype))
        .zone(orElse(zone, other.zone))
        .status(status.ordinal() > other.status.ordinal() ? status : other.status)
        .build();
  }

  @Override public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Instance instance = (Instance) o;
    return Objects.equals(node, instance.node) &&
        Objects.equals(privateIpAddress, instance.privateIpAddress) &&
        Objects.equals(vpcId, instance.vpcId) &&
        Objects.equals(subnetId, instance.subnetId) &&
        Objects.equals(ami, instance.ami) &&
        Objects.equals(vmtype, instance.vmtype) &&
        Objects.equals(zone, instance.zone) &&
        status == instance.status;
  }

  @Override public int hashCode() {
    return Objects.hash(node, privateIpAddress, vpcId, subnetId, ami, vmtype, zone, status);
  }

  @Override public String toString() {
    return "Instance("
        + "node=" + node + ", "
        + "privateIpAddress=" + privateIpAddress + ", "
        + "vpcId=" + vpcId + ", "
        + "subnetId=" + subnetId + ", "
        + "ami=" + ami + ", "
        + "vmtype=" + vmtype + ", "
        + "zone=" + zone + ", "
        + "status=" + status.name()
        + ")";
  }

  /** Return a builder that can be used to create an Instance. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for creating an Instance. */
  public static class Builder {
    private String node;
    private String privateIpAddress;
    private String vpcId;
    private String subnetId;
    private String ami;
    private String vmtype;
    private String zone;
    private Status status;

    private Builder() {
    }

    /** Set the instance ID. This attribute is required. */
    public Builder node(String value) {
      node = value;
      return this;
    }

    /** Set the IP address. This attribute is required. */
    public Builder privateIpAddress(String value) {
      privateIpAddress = value;
      return this;
    }

    /** Set the VPC ID. */
    public Builder vpcId(String value) {
      vpcId = value;
      return this;
    }

    /** Set the subnet ID. */
    public Builder subnetId(String value) {
      subnetId = value;
      return this;
    }

    /** Set the image ID. */
    public Builder ami(String value) {
      ami = value;
      return this;
    }

    /** Set the vmtype. */
    public Builder vmtype(String value) {
      vmtype = value;
      return this;
    }

    /** Set the availability zone. */
    public Builder zone(String value) {
      zone = value;
      return this;
    }

    /** Set the Eureka status. If not set, then {@link Status#NOT_REGISTERED} will be used. */
    public Builder status(Status value) {
      status = value;
      return this;
    }

    /** Create an instance from this builder. */
    public Instance build() {
      Preconditions.checkNotNull(node, "node must be set");
      Preconditions.checkNotNull(privateIpAddress, "privateIpAddress must be set");
      if (status == null) {
        status = Status.NOT_REGISTERED;
      }
      return new Instance(this);
    }
  }

  /** Represents the Eureka status. */
  public enum Status {
    /** Instance is not registered and heartbeating. */
    NOT_REGISTERED,

    /** Instance is sending a status of UP indicating it is ready for traffic. */
    UP,

    /** Instance is sending a status of DOWN indicating it should not get traffic. */
    DOWN,

    /** Instance is sending a status of STARTING and is not yet ready for traffic. */
    STARTING,

    /** Instance is marked as being out of service and should not receive traffic. */
    OUT_OF_SERVICE,

    /** Instance is registered, but the status is not known. */
    UNKNOWN
  }
}
