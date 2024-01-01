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

import com.netflix.spectator.api.Clock;
import com.netflix.spectator.api.Registry;
import com.typesafe.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * Setup registry for reporting spectator data to Atlas.
 */
@Configuration
@Conditional(NoSbnCondition.class)
public class AtlasConfiguration {

  @Bean
  AtlasRegistryService atlasRegistryService(Optional<Config> config) {
    return new AtlasRegistryService(Clock.SYSTEM, config.orElse(null));
  }

  @Bean
  Registry registry(AtlasRegistryService service) {
    return service.getRegistry();
  }
}

