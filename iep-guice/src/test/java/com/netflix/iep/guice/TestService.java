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
package com.netflix.iep.guice;

import com.netflix.iep.service.AbstractService;

import javax.inject.Inject;

public class TestService extends AbstractService {

  private boolean healthy = false;

  @Inject
  public TestService(Args args) {
    if (args.isEmpty()) {
      throw new IllegalArgumentException("missing required argument");
    } else {
      switch (args.get(0)) {
        case "fail":
          throw new IllegalArgumentException("die");
        case "up":
          healthy = true;
          break;
        case "down":
          healthy = false;
          break;
        default:
          throw new IllegalArgumentException("unknown mode: " + args.get(0));
      }
    }
  }

  @Override public boolean isHealthy() {
    return super.isHealthy() && healthy;
  }

  @Override protected void startImpl() throws Exception {
  }

  @Override protected void stopImpl() throws Exception {
  }
}
