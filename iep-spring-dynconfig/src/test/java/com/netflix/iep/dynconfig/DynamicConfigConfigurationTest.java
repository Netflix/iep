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
package com.netflix.iep.dynconfig;

import com.netflix.iep.config.ConfigManager;
import com.netflix.iep.config.DynamicConfigManager;
import com.netflix.iep.service.State;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class DynamicConfigConfigurationTest {

  @Test
  public void dynamicConfigManager() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(DynamicConfigConfiguration.class);
      context.refresh();
      context.start();
      DynamicConfigManager manager = context.getBean(DynamicConfigManager.class);
      Assert.assertSame(ConfigManager.dynamicConfigManager(), manager);
    }
  }

  @Test
  public void dynamicConfigService() throws Exception {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(DynamicConfigConfiguration.class);
      context.refresh();
      context.start();
      DynamicConfigService service = context.getBean(DynamicConfigService.class);
      Assert.assertEquals(State.RUNNING, service.state());
    }
  }
}
