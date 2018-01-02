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
package com.netflix.iep.rxnetty;


import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.config.SystemConfig;
import com.netflix.discovery.EurekaClient;
import com.netflix.iep.http.BasicServerRegistry;
import com.netflix.iep.http.EurekaServerRegistry;
import com.netflix.iep.http.RxHttp;
import com.netflix.iep.http.ServerRegistry;

import javax.inject.Singleton;


public final class RxNettyModule extends AbstractModule {

  private static class OptionalInjections {
    @Inject(optional = true)
    Config config;

    @Inject(optional = true)
    EurekaClient discovery;

    OptionalInjections() {
    }

    Config getConfig() {
      return (config == null)
          ? SystemConfig.INSTANCE
          : config;
    }

    ServerRegistry getServerRegistry() {
      return (discovery == null)
          ? new BasicServerRegistry()
          : new EurekaServerRegistry(discovery);
    }
  }

  @Override protected void configure() {
  }

  @Provides
  @Singleton
  private ServerRegistry providesServerRegistry(OptionalInjections opts) {
    return opts.getServerRegistry();
  }

  @Provides
  @Singleton
  private RxHttp providesRxHttp(OptionalInjections opts, ServerRegistry registry) {
    return new RxHttp(opts.getConfig(), registry);
  }

  @Override public boolean equals(Object obj) {
    return obj != null && getClass().equals(obj.getClass());
  }

  @Override public int hashCode() {
    return getClass().hashCode();
  }
}
