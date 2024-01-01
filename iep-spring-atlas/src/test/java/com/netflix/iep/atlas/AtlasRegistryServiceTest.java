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
package com.netflix.iep.atlas;

import com.netflix.spectator.atlas.AtlasConfig;
import com.netflix.spectator.atlas.AtlasRegistry;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;
import java.util.Map;

@RunWith(JUnit4.class)
public class AtlasRegistryServiceTest {

  @Test
  public void validTagCharacters() throws Exception {
    AtlasRegistryService service = new AtlasRegistryService(null, null);
    AtlasRegistry registry = (AtlasRegistry) service.getRegistry();
    AtlasConfig config = (AtlasConfig) registry.config();
    Assert.assertEquals("-._A-Za-z0-9", config.validTagCharacters());
    service.stop();
  }

  private Config createConfig(String... data) {
    Map<String, Object> props = new HashMap<>();
    for (int i = 0; i < data.length; i += 2) {
      props.put(data[i], data[i + 1]);
    }
    props.put("netflix.iep.atlas.collection.gc", "false");
    props.put("netflix.iep.atlas.collection.jvm", "false");
    return ConfigFactory.parseMap(props);
  }

  private AtlasRegistry createRegistry(Config config) {
    AtlasRegistryService service = new AtlasRegistryService(null, config);
    return (AtlasRegistry) service.getRegistry();
  }

  @Test
  public void disabledIfNull() {
    Config config = createConfig();
    try (AtlasRegistry registry = createRegistry(config)) {
      Assert.assertFalse(((AtlasConfig) registry.config()).enabled());
    }
  }

  @Test
  public void disabledIfLocal() {
    Config config = createConfig("netflix.iep.env.host", "localhost");
    try (AtlasRegistry registry = createRegistry(config)) {
      Assert.assertFalse(((AtlasConfig) registry.config()).enabled());
    }
  }

  @Test
  public void enabledIfNotLocal() {
    Config config = createConfig("netflix.iep.env.host", "10.0.0.1");
    try (AtlasRegistry registry = createRegistry(config)) {
      Assert.assertTrue(((AtlasConfig) registry.config()).enabled());
    }
  }

  @Test
  public void explicitEnableOverridesLocal() {
    Config config = createConfig(
        "netflix.iep.env.host", "localhost",
        "netflix.iep.atlas.enabled", "true"
    );
    try (AtlasRegistry registry = createRegistry(config)) {
      Assert.assertTrue(((AtlasConfig) registry.config()).enabled());
    }
  }
}
