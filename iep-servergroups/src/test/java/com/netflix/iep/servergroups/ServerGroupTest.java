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

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collections;

@RunWith(JUnit4.class)
public class ServerGroupTest {

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(ServerGroup.class)
        .verify();
  }

  @Test(expected = NullPointerException.class)
  public void missingPlatform() {
    ServerGroup.builder()
        .group("app-main-v001")
        .build();
  }

  @Test(expected = NullPointerException.class)
  public void missingGroup() {
    ServerGroup.builder()
        .platform("titus")
        .build();
  }

  private Instance defaultInstance() {
    return Instance.builder()
        .node("i-12345")
        .privateIpAddress("1.2.3.4")
        .vpcId("vpc-123")
        .subnetId("subnet-123")
        .ami("ami-123")
        .vmtype("m5.large")
        .zone("us-east-1e")
        .status(Instance.Status.STARTING)
        .build();
  }

  private ServerGroup defaultGroup() {
    return ServerGroup.builder()
        .platform("ec2")
        .group("app-stack-detail-v001")
        .minSize(10)
        .maxSize(100)
        .desiredSize(42)
        .addInstance(defaultInstance())
        .build();
  }

  @Test
  public void id() {
    Assert.assertEquals("ec2.app-stack-detail-v001", defaultGroup().getId());
  }

  @Test
  public void platform() {
    Assert.assertEquals("ec2", defaultGroup().getPlatform());
  }

  @Test
  public void app() {
    Assert.assertEquals("app", defaultGroup().getApp());
  }

  @Test
  public void cluster() {
    Assert.assertEquals("app-stack-detail", defaultGroup().getCluster());
  }

  @Test
  public void group() {
    Assert.assertEquals("app-stack-detail-v001", defaultGroup().getGroup());
  }

  @Test
  public void stack() {
    Assert.assertEquals("stack", defaultGroup().getStack());
  }

  @Test
  public void detail() {
    Assert.assertEquals("detail", defaultGroup().getDetail());
  }

  @Test
  public void minSize() {
    Assert.assertEquals(10, defaultGroup().getMinSize());
  }

  @Test
  public void maxSize() {
    Assert.assertEquals(100, defaultGroup().getMaxSize());
  }

  @Test
  public void desiredSize() {
    Assert.assertEquals(42, defaultGroup().getDesiredSize());
  }

  @Test
  public void instances() {
    Assert.assertEquals(Collections.singletonList(defaultInstance()), defaultGroup().getInstances());
  }

  @Test(expected = IllegalArgumentException.class)
  public void mergeDifferentGroups() {
    ServerGroup g1 = ServerGroup.builder()
        .platform("ec2")
        .group("app")
        .build();
    ServerGroup g2 = ServerGroup.builder()
        .platform("ec2")
        .group("app2")
        .build();
    g1.merge(g2);
  }

  @Test
  public void mergePreferMaxSize() {
    ServerGroup g1 = ServerGroup.builder()
        .platform("ec2")
        .group("app-stack-detail-v001")
        .minSize(10)
        .maxSize(100)
        .desiredSize(42)
        .build();
    ServerGroup g2 = ServerGroup.builder()
        .platform("ec2")
        .group("app-stack-detail-v001")
        .minSize(5)
        .maxSize(200)
        .desiredSize(21)
        .build();
    ServerGroup expected = ServerGroup.builder()
        .platform("ec2")
        .group("app-stack-detail-v001")
        .minSize(10)
        .maxSize(200)
        .desiredSize(42)
        .build();
    Assert.assertEquals(expected, g1.merge(g2));
  }

  @Test
  public void mergeSameInstances() {
    ServerGroup g1 = ServerGroup.builder()
        .platform("ec2")
        .group("app-stack-detail-v001")
        .addInstance(Instance.builder()
            .node("i-12345")
            .privateIpAddress("1.2.3.4")
            .status(Instance.Status.NOT_REGISTERED)
            .build())
        .build();
    ServerGroup g2 = ServerGroup.builder()
        .platform("ec2")
        .group("app-stack-detail-v001")
        .addInstance(Instance.builder()
            .node("i-12345")
            .privateIpAddress("1.2.3.4")
            .status(Instance.Status.UP)
            .build())
        .build();
    Assert.assertEquals(g2, g1.merge(g2));
  }

  @Test
  public void mergeOtherInstancesUp() {
    ServerGroup g1 = ServerGroup.builder()
        .platform("ec2")
        .group("app-stack-detail-v001")
        .build();
    ServerGroup g2 = ServerGroup.builder()
        .platform("ec2")
        .group("app-stack-detail-v001")
        .addInstance(Instance.builder()
            .node("i-12345")
            .privateIpAddress("1.2.3.4")
            .status(Instance.Status.UP)
            .build())
        .build();
    Assert.assertEquals(g2, g1.merge(g2));
  }

  @Test
  public void mergeOtherInstancesStarting() {
    ServerGroup g1 = ServerGroup.builder()
        .platform("ec2")
        .group("app-stack-detail-v001")
        .build();
    ServerGroup g2 = ServerGroup.builder()
        .platform("ec2")
        .group("app-stack-detail-v001")
        .addInstance(Instance.builder()
            .node("i-12345")
            .privateIpAddress("1.2.3.4")
            .status(Instance.Status.STARTING)
            .build())
        .build();
    Assert.assertEquals(g1, g1.merge(g2));
    Assert.assertEquals(g1, g2.merge(g1));
  }

  @Test
  public void mergeList() {
    ServerGroup g1 = ServerGroup.builder()
        .platform("ec2")
        .group("app-stack-detail-v001")
        .build();
    ServerGroup g2 = ServerGroup.builder()
        .platform("ec2")
        .group("app-stack-detail-v001")
        .addInstance(Instance.builder()
            .node("i-12345")
            .privateIpAddress("1.2.3.4")
            .status(Instance.Status.UP)
            .build())
        .build();
    Assert.assertEquals(
        Collections.singletonList(g2),
        ServerGroup.merge(Collections.singletonList(g2), Collections.singletonList(g1)));
  }
}
