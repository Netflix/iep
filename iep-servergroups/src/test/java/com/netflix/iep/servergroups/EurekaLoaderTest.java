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

import com.netflix.spectator.ipc.http.HttpClient;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RunWith(JUnit4.class)
public class EurekaLoaderTest {

  private List<ServerGroup> get(String resource) throws Exception {
    return get(resource, null);
  }

  private List<ServerGroup> get(String resource, String account) throws Exception {
    List<ServerGroup> groups = LoaderUtils.createEurekaLoader(resource, account).call();
    groups.sort(Comparator.comparing(ServerGroup::getId));
    return groups;
  }

  private ServerGroup defaultEc2Group() {
    return ServerGroup.builder()
        .platform("ec2")
        .group("app-main-v001")
        .addInstance(Instance.builder()
            .node("i-1234567890")
            .privateIpAddress("10.20.30.40")
            .vpcId("vpc-54321")
            .ami("ami-0987654321")
            .vmtype("m5.large")
            .zone("us-east-1d")
            .status(Instance.Status.UP)
            .build())
        .build();
  }

  @Test
  public void ec2Group() throws Exception {
    List<ServerGroup> expected = new ArrayList<>();
    expected.add(defaultEc2Group());
    List<ServerGroup> actual = get("eureka-ec2.json");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void ec2GroupAccountFilterMatches() throws Exception {
    List<ServerGroup> expected = new ArrayList<>();
    expected.add(defaultEc2Group());
    List<ServerGroup> actual = get("eureka-ec2.json", "12345");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void ec2GroupAccountFilterDoesNotMatch() throws Exception {
    List<ServerGroup> expected = new ArrayList<>();
    List<ServerGroup> actual = get("eureka-ec2.json", "54321");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void preferIpAddrToMetadataLocalIp() throws Exception {
    List<ServerGroup> expected = new ArrayList<>();
    expected.add(ServerGroup.builder()
        .platform("ec2")
        .group("app-main-v001")
        .addInstance(Instance.builder()
            .node("i-1234567890")
            .privateIpAddress("10.20.30.40")
            .ipv6Address("::ffff:a14:1e2a")
            .vpcId("vpc-54321")
            .ami("ami-0987654321")
            .vmtype("m5.large")
            .zone("us-east-1d")
            .status(Instance.Status.UP)
            .build())
        .build()); // metadata has 10.20.30.42
    List<ServerGroup> actual = get("eureka-ip.json", "12345");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void noInstanceId() throws Exception {
    List<ServerGroup> expected = new ArrayList<>();
    expected.add(defaultEc2Group());
    List<ServerGroup> actual = get("eureka-no-id.json", "12345");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void noIpAddr() throws Exception {
    List<ServerGroup> expected = new ArrayList<>();
    expected.add(defaultEc2Group());
    List<ServerGroup> actual = get("eureka-no-ip.json", "12345");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void duplicateRegistration() throws Exception {
    List<ServerGroup> expected = new ArrayList<>();
    expected.add(defaultEc2Group());
    List<ServerGroup> actual = get("eureka-dup.json", "12345");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void titusGroup() throws Exception {
    List<ServerGroup> expected = new ArrayList<>();
    expected.add(ServerGroup.builder()
        .platform("titus")
        .group("app-main-v001")
        .addInstance(Instance.builder()
            .node("8d78c384-aa57-48cc-ad4d-a1a887f46221")
            .privateIpAddress("10.20.30.40")
            .vpcId("vpc-54321")
            .zone("us-east-1d")
            .status(Instance.Status.UP)
            .build())
        .build());
    List<ServerGroup> actual = get("eureka-titus.json", "12345");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void titusGroupVmtype() throws Exception {
    List<ServerGroup> expected = new ArrayList<>();
    expected.add(ServerGroup.builder()
        .platform("titus")
        .group("app-main-v001")
        .addInstance(Instance.builder()
            .node("8d78c384-aa57-48cc-ad4d-a1a887f46221")
            .privateIpAddress("10.20.30.40")
            .vpcId("vpc-54321")
            .zone("us-east-1d")
            .status(Instance.Status.UP)
            .build())
        .build());
    List<ServerGroup> actual = get("eureka-titus-2.json", "12345");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void invalidInstance() throws Exception {
    List<ServerGroup> expected = new ArrayList<>();
    List<ServerGroup> actual = get("eureka-invalid.json");
    Assert.assertEquals(expected, actual);
  }

  @Test(expected = IOException.class)
  public void failedRequest() throws Exception {
    HttpClient client = TestHttpClient.empty(400);
    EurekaLoader loader = new EurekaLoader(client, LoaderUtils.EUREKA_URI, v -> true);
    loader.call();
  }
}
