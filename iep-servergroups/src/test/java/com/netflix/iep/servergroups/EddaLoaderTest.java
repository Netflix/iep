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
package com.netflix.iep.servergroups;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RunWith(JUnit4.class)
public class EddaLoaderTest {

  private List<ServerGroup> get(String resource) throws Exception {
    List<ServerGroup> groups = LoaderUtils.createEddaLoader(resource).call();
    groups.sort(Comparator.comparing(ServerGroup::getId));
    return groups;
  }

  private ServerGroup defaultEc2Group() {
    return ServerGroup.builder()
        .platform("ec2")
        .group("app-main-dev-v001")
        .minSize(1)
        .maxSize(3)
        .desiredSize(2)
        .addInstance(Instance.builder()
            .node("i-1234567890")
            .privateIpAddress("10.20.30.40")
            .vpcId("vpc-54321")
            .subnetId("subnet-54321")
            .ami("ami-0987654321")
            .vmtype("m5.large")
            .zone("us-east-1d")
            .status(Instance.Status.NOT_REGISTERED)
            .build())
        .build();
  }

  @Test
  public void ec2Group() throws Exception {
    List<ServerGroup> expected = new ArrayList<>();
    expected.add(defaultEc2Group());
    List<ServerGroup> actual = get("edda-ec2.json");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void overflow() throws Exception {
    List<ServerGroup> expected = new ArrayList<>();
    expected.add(ServerGroup.builder()
        .platform("ec2")
        .group("app-main-dev-v001")
        .minSize(-1)
        .maxSize(-1)
        .desiredSize(-1)
        .addInstance(Instance.builder()
            .node("i-1234567890")
            .privateIpAddress("10.20.30.40")
            .vpcId("vpc-54321")
            .subnetId("subnet-54321")
            .ami("ami-0987654321")
            .vmtype("m5.large")
            .zone("us-east-1d")
            .status(Instance.Status.NOT_REGISTERED)
            .build())
        .build());
    List<ServerGroup> actual = get("edda-overflow.json");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void noInstances() throws Exception {
    List<ServerGroup> expected = new ArrayList<>();
    expected.add(ServerGroup.builder()
        .platform("ec2")
        .group("app-main-dev-v001")
        .minSize(1)
        .maxSize(3)
        .desiredSize(2)
        .build());
    List<ServerGroup> actual = get("edda-no-instances.json");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void noIP() throws Exception {
    List<ServerGroup> expected = new ArrayList<>();
    expected.add(defaultEc2Group());
    List<ServerGroup> actual = get("edda-noip.json");
    Assert.assertEquals(expected, actual);
  }
}
