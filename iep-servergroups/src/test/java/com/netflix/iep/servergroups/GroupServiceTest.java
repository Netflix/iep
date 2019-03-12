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
package com.netflix.iep.servergroups;

import com.netflix.spectator.api.NoopRegistry;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@RunWith(JUnit4.class)
public class GroupServiceTest {

  private Instance eddaInstance() {
    return Instance.builder()
        .node("i-12345")
        .privateIpAddress("1.2.3.4")
        .vpcId("vpc-123")
        .subnetId("subnet-123")
        .ami("ami-123")
        .vmtype("m5.large")
        .zone("us-east-1e")
        .status(Instance.Status.NOT_REGISTERED)
        .build();
  }

  private ServerGroup eddaGroup() {
    return ServerGroup.builder()
        .platform("ec2")
        .group("app-stack-detail-v001")
        .minSize(10)
        .maxSize(100)
        .desiredSize(42)
        .addInstance(eddaInstance())
        .build();
  }

  private Instance eurekaInstance() {
    return Instance.builder()
        .node("i-12345")
        .privateIpAddress("1.2.3.4")
        .vpcId("vpc-123")
        .subnetId("subnet-123")
        .ami("ami-123")
        .vmtype("m5.large")
        .zone("us-east-1e")
        .status(Instance.Status.NOT_REGISTERED)
        .build();
  }

  private ServerGroup eurekaGroup() {
    return ServerGroup.builder()
        .platform("ec2")
        .group("app-stack-detail-v001")
        .minSize(10)
        .maxSize(100)
        .desiredSize(42)
        .addInstance(eurekaInstance())
        .build();
  }

  private Loader eddaLoader() {
    return () -> Collections.singletonList(eddaGroup());
  }

  @Test
  public void start() throws Exception {
    Map<String, Loader> loaders = new LinkedHashMap<>();
    loaders.put("test", eddaLoader());
    GroupService service = new GroupService(new NoopRegistry(), Duration.ZERO, loaders);
    service.start();
    Assert.assertEquals(Collections.singletonList(eddaGroup()), service.getGroups());
    service.stop();
  }

  @Test
  public void failureToLoad() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    Map<String, Loader> loaders = new LinkedHashMap<>();
    loaders.put("edda", eddaLoader());
    loaders.put("eureka", () -> {
      if (latch.getCount() > 0) {
        latch.countDown();
        throw new RuntimeException();
      }
      return Collections.singletonList(eurekaGroup());
    });
    GroupService service = new GroupService(new NoopRegistry(), Duration.ZERO, loaders);
    service.start();
    Assert.assertEquals(Collections.singletonList(eddaGroup()), service.getGroups());
    service.stop();
  }
}
