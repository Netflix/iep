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
package com.netflix.iep.aws2;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a binding for an {@link AwsClientFactory}.
 */
public final class AwsModule extends AbstractModule {

  private static final Logger LOGGER = LoggerFactory.getLogger(AwsModule.class);

  private static class OptionalInjections {
    @Inject(optional = true)
    private Config config;

    Config getConfig() {
      return (config == null) ? ConfigFactory.load(pickClassLoader()) : config;
    }

    private ClassLoader pickClassLoader() {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      if (cl == null) {
        LOGGER.warn("Thread.currentThread().getContextClassLoader() is null, using loader for {}",
            getClass().getName());
        cl = getClass().getClassLoader();
      }
      return cl;
    }
  }

  @Override protected void configure() {
  }

  @Provides
  private AwsClientFactory providesClientFactory(OptionalInjections opts) {
    return new AwsClientFactory(opts.getConfig());
  }

  @Override public boolean equals(Object obj) {
    return obj != null && getClass().equals(obj.getClass());
  }

  @Override public int hashCode() {
    return getClass().hashCode();
  }
}
