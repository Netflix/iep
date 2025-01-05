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
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

@SuppressWarnings("unchecked")
public class PropsEndpointTest {

  @Before
  public void before() {
    ConfigManager.dynamicConfigManager().setOverrideConfig(ConfigFactory.empty());
  }

  @Test
  public void getAll() {
    PropsEndpoint endpoint = new PropsEndpoint(ConfigManager.dynamicConfigManager());
    Map<String, String> props = (Map<String, String>) endpoint.get();
    Assert.assertEquals("app", props.get("iep.test.which-config"));
  }

  @Test
  public void getPath() {
    PropsEndpoint endpoint = new PropsEndpoint(ConfigManager.dynamicConfigManager());
    Map<String, String> props = (Map<String, String>) endpoint.get("iep.test");
    Assert.assertEquals("app", props.get("which-config"));
  }
}
