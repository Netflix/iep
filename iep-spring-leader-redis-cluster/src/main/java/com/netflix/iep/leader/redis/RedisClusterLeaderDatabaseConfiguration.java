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
package com.netflix.iep.leader.redis;

import com.netflix.iep.leader.api.LeaderDatabase;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class RedisClusterLeaderDatabaseConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(RedisClusterLeaderDatabaseConfiguration.class);

    @Bean
    LeaderDatabase leaderDatabase(
        RedisClusterLeaderClient client,
        Optional<Config> config
    ) {
        Config c = config.orElseGet(ConfigFactory::load);
        return new RedisClusterLeaderDatabase(c, client);
    }

    @Bean
    RedisClusterLeaderClient redisCluster(Optional<Config> config) {
        Config c = config.orElseGet(ConfigFactory::load);
        return new RedisClusterLeaderClient(c);
    }

}
