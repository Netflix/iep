/*
 * Copyright 2014-2025 Netflix, Inc.
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
package com.netflix.iep.sbhealth;

import com.netflix.iep.service.Service;
import com.netflix.iep.service.State;
import com.netflix.iep.spring.IepConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.atomic.AtomicBoolean;

public class IepServiceHealthIndicatorTest {

  @Test
  public void springBootHealth() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      TestService service = new TestService();
      context.register(IepConfiguration.class);
      context.registerBean("testService", TestService.class, () -> service);
      context.registerBean(IepServiceHealthIndicator.class);
      context.refresh();
      context.start();

      HealthIndicator health = context.getBean(HealthIndicator.class);
      Assert.assertTrue(health instanceof IepServiceHealthIndicator);
      Assert.assertEquals(Status.DOWN, health.health().getStatus());

      service.setRunning(true);
      Assert.assertEquals(Status.UP, health.health().getStatus());
    }
  }

  private static final class TestService implements Service {

    private final AtomicBoolean running = new AtomicBoolean(false);

    TestService() {
    }

    @Override
    public String name() {
      return getClass().getSimpleName();
    }

    @Override
    public boolean isHealthy() {
      return state() == State.RUNNING;
    }

    @Override
    public State state() {
      return running.get() ? State.RUNNING : State.STARTING;
    }

    void setRunning(boolean running) {
      this.running.set(running);
    }
  }
}
