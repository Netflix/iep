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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.iep.config.ConfigManager;
import com.netflix.iep.config.DynamicConfigManager;
import org.junit.Assert;
import org.junit.Test;

public class DynamicConfigModuleTest {

  @Test
  public void dynamicConfigManager() {
    Injector injector = Guice.createInjector(new DynamicConfigModule());
    DynamicConfigManager manager = injector.getInstance(DynamicConfigManager.class);
    Assert.assertSame(ConfigManager.dynamicConfigManager(), manager);
  }

  @Test
  public void dynamicConfigService() throws Exception {
    Injector injector = Guice.createInjector(new DynamicConfigModule());
    DynamicConfigService service = injector.getInstance(DynamicConfigService.class);
    service.start();
    service.stop();
  }
}
