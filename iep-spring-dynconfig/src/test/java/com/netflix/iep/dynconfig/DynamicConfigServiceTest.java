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
package com.netflix.iep.dynconfig;

import com.netflix.iep.config.ConfigManager;
import com.netflix.iep.config.DynamicConfigManager;
import com.netflix.spectator.api.NoopRegistry;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Properties;

public class DynamicConfigServiceTest {

  private final DynamicConfigManager manager = ConfigManager.dynamicConfigManager();

  private DynamicConfigService newService() {
    return new DynamicConfigService(new NoopRegistry(), ConfigManager.get());
  }

  @Before
  public void before() {
    manager.setOverrideConfig(ConfigFactory.empty());
  }

  @Test
  public void fastProperties() {
    Properties props = new Properties();
    props.setProperty("iep.test.which-config", "dynamic");
    DynamicConfigService service = newService();
    service.updateDynamicConfig(props);
    Assert.assertEquals("dynamic", manager.get().getString("iep.test.which-config"));
  }

  @Test
  public void override() {
    Properties props = new Properties();
    props.setProperty("iep.test.which-config", "dynamic");
    props.setProperty("a", "foo");
    props.setProperty("netflix.iep.override", "iep.test.which-config=override\nb=[1]");
    DynamicConfigService service = newService();
    service.updateDynamicConfig(props);
    Assert.assertFalse(manager.get().hasPath("netflix.iep.override"));
    Assert.assertEquals("override", manager.get().getString("iep.test.which-config"));
    Assert.assertEquals("foo", manager.get().getString("a"));
    Assert.assertEquals(Collections.singletonList(1), manager.get().getIntList("b"));
  }

  @Test
  public void overrideSubstitution() {
    Properties props = new Properties();
    props.setProperty("netflix.iep.override", "value=${includes.a}\"_\"${includes.b}");
    DynamicConfigService service = newService();
    service.updateDynamicConfig(props);
    Assert.assertEquals("abc_def:foo", manager.get().getString("value"));
  }

  @Test
  public void overrideComplex() {
    Config override = ConfigFactory.parseResources("override.conf");
    Properties props = new Properties();
    props.setProperty("netflix.iep.override", override.root().render());
    DynamicConfigService service = newService();
    service.updateDynamicConfig(props);
    Assert.assertEquals(override.getConfig("ieptest"), manager.get().getConfig("ieptest"));
  }
}
