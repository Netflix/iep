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
package com.netflix.iep.dynconfig;

import com.netflix.iep.admin.EndpointMapping;
import com.netflix.iep.config.ConfigManager;
import com.netflix.iep.config.DynamicConfigManager;
import com.netflix.spectator.api.NoopRegistry;
import com.netflix.spectator.api.Registry;
import com.typesafe.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * Configures the {@link ConfigManager#dynamicConfigManager()} to update the override layer
 * with properties from a remote property service.
 */
@Configuration
public class DynamicConfigConfiguration {

  @Bean
  Config config() {
    return ConfigManager.get();
  }

  @Bean
  DynamicConfigManager dynamicConfigManager(DynamicConfigService service) {
    // The service parameter is necessary to ensure that the dependency injector will
    // create the service and update the remote layer before injecting the DynamicConfigManager
    // into other objects.
    return ConfigManager.dynamicConfigManager();
  }

  @Bean
  DynamicConfigService dynamicConfigService(Optional<Registry> registry, Config config) {
    Registry r = registry.orElseGet(NoopRegistry::new);
    return new DynamicConfigService(r, config);
  }

  @Bean
  EndpointMapping propsEndpointMapping(DynamicConfigManager manager) {
    return new EndpointMapping("/props", new PropsEndpoint(manager));
  }
}
