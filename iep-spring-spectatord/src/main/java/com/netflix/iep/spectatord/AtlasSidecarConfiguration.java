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
package com.netflix.iep.spectatord;

import com.netflix.spectator.api.Registry;
import com.typesafe.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * Setup registry for reporting spectator data to Atlas.
 */
@Configuration
public class AtlasSidecarConfiguration {

  @Bean
  SidecarRegistryService statelessRegistryService(Optional<Config> config) {
    return new SidecarRegistryService(config.orElse(null));
  }

  @Bean
  Registry registry(SidecarRegistryService service) {
    return service.getRegistry();
  }
}

