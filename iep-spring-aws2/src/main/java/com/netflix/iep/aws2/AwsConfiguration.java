/*
 * Copyright 2014-2023 Netflix, Inc.
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
package com.netflix.iep.aws2;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * Creates a binding for an {@link AwsClientFactory}.
 */
@Configuration
public class AwsConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(AwsConfiguration.class);

  private ClassLoader pickClassLoader() {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) {
      LOGGER.warn("Thread.currentThread().getContextClassLoader() is null, using loader for {}",
          getClass().getName());
      cl = getClass().getClassLoader();
    }
    return cl;
  }

  @Bean
  AwsClientFactory providesClientFactory(Optional<Config> config) {
    Config c = config.orElseGet(() -> ConfigFactory.load(pickClassLoader()));
    return new AwsClientFactory(c);
  }
}
