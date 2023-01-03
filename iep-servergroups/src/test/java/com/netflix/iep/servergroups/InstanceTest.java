/*
 * Copyright 2014-2023 Netflix, Inc.
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

@RunWith(JUnit4.class)
public class InstanceTest {

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(Instance.class)
        .verify();
  }

  @Test(expected = NullPointerException.class)
  public void missingNode() {
    Instance.builder()
        .privateIpAddress("1.2.3.4")
        .build();
  }

  @Test(expected = NullPointerException.class)
  public void missingPrivateIp() {
    Instance.builder()
        .node("i-12345")
        .build();
  }

  @Test
  public void missingStatus() {
    Instance instance = Instance.builder()
        .node("i-12345")
        .privateIpAddress("1.2.3.4")
        .build();
    Assert.assertEquals(Instance.Status.NOT_REGISTERED, instance.getStatus());
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

  @Test
  public void node() {
    Assert.assertEquals("i-12345", defaultInstance().getNode());
  }

  @Test
  public void privateIpAddress() {
    Assert.assertEquals("1.2.3.4", defaultInstance().getPrivateIpAddress());
  }

  @Test
  public void vpcId() {
    Assert.assertEquals("vpc-123", defaultInstance().getVpcId());
  }

  @Test
  public void subnetId() {
    Assert.assertEquals("subnet-123", defaultInstance().getSubnetId());
  }

  @Test
  public void ami() {
    Assert.assertEquals("ami-123", defaultInstance().getAmi());
  }

  @Test
  public void vmtype() {
    Assert.assertEquals("m5.large", defaultInstance().getVmtype());
  }

  @Test
  public void zone() {
    Assert.assertEquals("us-east-1e", defaultInstance().getZone());
  }

  @Test(expected = IllegalArgumentException.class)
  public void mergeDifferentInstances() {
    Instance i1 = Instance.builder()
        .node("i-12345")
        .privateIpAddress("1.2.3.4")
        .build();
    Instance i2 = Instance.builder()
        .node("i-54321")
        .privateIpAddress("4.3.2.1")
        .build();
    i1.merge(i2);
  }

  @Test
  public void mergePreferThis() {
    Instance i1 = Instance.builder()
        .node("i-12345")
        .privateIpAddress("1.2.3.4")
        .vpcId("vpc-123")
        .subnetId("subnet-123")
        .ami("ami-123")
        .vmtype("m5.large")
        .zone("us-east-1e")
        .status(Instance.Status.NOT_REGISTERED)
        .build();
    Instance i2 = Instance.builder()
        .node("i-12345")
        .privateIpAddress("4.3.2.1")
        .vpcId("vpc-321")
        .subnetId("subnet-321")
        .ami("ami-321")
        .vmtype("m5.xlarge")
        .zone("us-east-1c")
        .status(Instance.Status.NOT_REGISTERED)
        .build();
    Instance merged = i1.merge(i2);
    Assert.assertEquals(i1, merged);
  }

  @Test
  public void mergeFillInWithOther() {
    Instance i1 = Instance.builder()
        .node("i-12345")
        .privateIpAddress("1.2.3.4")
        .status(Instance.Status.NOT_REGISTERED)
        .build();
    Instance i2 = Instance.builder()
        .node("i-12345")
        .privateIpAddress("1.2.3.4")
        .vpcId("vpc-321")
        .subnetId("subnet-321")
        .ami("ami-321")
        .vmtype("m5.xlarge")
        .zone("us-east-1c")
        .status(Instance.Status.NOT_REGISTERED)
        .build();
    Instance merged = i1.merge(i2);
    Assert.assertEquals(i2, merged);
  }

  @Test
  public void mergeHigherStatusWins() {
    Instance.Status[] statuses = Instance.Status.values();
    for (int i = 1; i < statuses.length; ++i) {
      Instance i1 = Instance.builder()
          .node("i-12345")
          .privateIpAddress("1.2.3.4")
          .status(statuses[i - 1])
          .build();
      Instance i2 = Instance.builder()
          .node("i-12345")
          .privateIpAddress("1.2.3.4")
          .status(statuses[i])
          .build();
      Instance merged = i1.merge(i2);
      Assert.assertEquals(i2, merged);
    }
  }
}
