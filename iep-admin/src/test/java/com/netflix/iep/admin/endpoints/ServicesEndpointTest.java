/*
 * Copyright 2014-2018 Netflix, Inc.
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
package com.netflix.iep.admin.endpoints;

import com.netflix.iep.service.Service;
import com.netflix.iep.service.ServiceManager;
import com.netflix.iep.service.State;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@RunWith(JUnit4.class)
public class ServicesEndpointTest {

  private static Set<Service> services() {
    Set<Service> ss = new HashSet<>();

    ss.add(new Service() {
      @Override public String name() {
        return "stopping-service";
      }

      @Override public boolean isHealthy() {
        return false;
      }

      @Override public State state() {
        return State.STOPPING;
      }
    });

    ss.add(new Service() {
      @Override public String name() {
        return "running-service";
      }

      @Override public boolean isHealthy() {
        return true;
      }

      @Override public State state() {
        return State.RUNNING;
      }
    });

    return ss;
  }

  private final ServicesEndpoint endpoint = new ServicesEndpoint(new ServiceManager(services()));

  @Test @SuppressWarnings("unchecked")
  public void get() {
    List<Map<String, Object>> status = (List<Map<String, Object>>) endpoint.get();
    Assert.assertEquals(2, status.size());
    for (Map<String, Object> s : status) {
      switch (s.get("name").toString()) {
        case "running-service":
          Assert.assertEquals(Boolean.TRUE, s.get("healthy"));
          Assert.assertEquals(State.RUNNING.name(), s.get("state"));
          break;
        case "stopping-service":
          Assert.assertEquals(Boolean.FALSE, s.get("healthy"));
          Assert.assertEquals(State.STOPPING.name(), s.get("state"));
          break;
        default:
          Assert.fail();
          break;
      }
    }
  }

  @Test
  public void getWithPath() {
    // Should always return null, path isn't supported
    Assert.assertNull(endpoint.get("running-service"));
  }
}
