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
package com.netflix.iep.leader;

import com.netflix.iep.leader.api.LeaderDatabase;
import com.netflix.iep.leader.api.LeaderElector;
import com.netflix.iep.leader.api.LeaderId;
import com.netflix.iep.leader.api.LeaderStatus;
import com.netflix.iep.leader.api.ResourceId;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class LeaderConfigurationTest {

  private LeaderDatabase createLeaderDatabase() {
    return new LeaderDatabase() {
      @Override
      public void initialize() {
      }

      @Override
      public LeaderId getLeaderFor(ResourceId resourceId) {
        return LeaderId.NO_LEADER;
      }

      @Override
      public boolean updateLeadershipFor(ResourceId resourceId) {
        return false;
      }

      @Override
      public boolean removeLeadershipFor(ResourceId resourceId) {
        return false;
      }
    };
  }

  @Test
  public void leaderElector() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(LeaderConfiguration.class);
      context.registerBean(LeaderDatabase.class, this::createLeaderDatabase);
      context.refresh();
      context.start();
      Assert.assertNotNull(context.getBean(LeaderElector.class));
    }
  }

  @Test
  public void leaderStatus() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(LeaderConfiguration.class);
      context.registerBean(LeaderDatabase.class, this::createLeaderDatabase);
      context.refresh();
      context.start();
      Assert.assertNotNull(context.getBean(LeaderStatus.class));
    }
  }

  @Test
  public void leaderService() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(LeaderConfiguration.class);
      context.registerBean(LeaderDatabase.class, this::createLeaderDatabase);
      context.refresh();
      context.start();
      Assert.assertNotNull(context.getBean(LeaderService.class));
    }
  }
}
