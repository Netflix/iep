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
package com.netflix.iep.leader;

import com.netflix.iep.leader.api.LeaderDatabase;
import com.netflix.iep.leader.api.LeaderElector;
import com.netflix.iep.leader.api.LeaderStatus;
import com.netflix.spectator.api.NoopRegistry;
import com.netflix.spectator.api.Registry;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class LeaderConfiguration {

  @Bean
  StandardLeaderElector leaderElector(
      LeaderDatabase db, Optional<Registry> registry, Optional<Config> config) {
    Registry r = registry.orElseGet(NoopRegistry::new);
    Config c = config.orElseGet(ConfigFactory::load);
    return new StandardLeaderElector(db, c, r);
  }

  @Bean
  LeaderService leaderService(
      LeaderElector elector, Optional<Registry> registry, Optional<Config> config) {
    Registry r = registry.orElseGet(NoopRegistry::new);
    Config c = config.orElseGet(ConfigFactory::load);
    return new LeaderService(elector, c, r);
  }
}
