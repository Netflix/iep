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
package com.netflix.iep.atlas;

import com.netflix.spectator.atlas.AtlasConfig;
import com.netflix.spectator.atlas.AtlasRegistry;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AtlasRegistryServiceTest {

  @Test
  public void validTagCharacters() throws Exception {
    AtlasRegistryService service = new AtlasRegistryService();
    AtlasRegistry registry = (AtlasRegistry) service.getRegistry();
    AtlasConfig config = (AtlasConfig) registry.config();
    Assert.assertEquals("-._A-Za-z0-9", config.validTagCharacters());
    Assert.assertEquals("-._A-Za-z0-9^~", config.validTagValueCharacters().get("nf.cluster"));
    Assert.assertEquals("-._A-Za-z0-9^~", config.validTagValueCharacters().get("nf.asg"));
    Assert.assertNull(config.validTagValueCharacters().get("nf.zone"));
    service.stop();
  }
}
