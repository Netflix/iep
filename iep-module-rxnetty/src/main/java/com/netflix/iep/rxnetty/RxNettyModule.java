/*
 * Copyright 2015 Netflix, Inc.
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
import com.google.inject.Provides;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.iep.http.EurekaServerRegistry;
import com.netflix.iep.http.RxHttp;
import com.netflix.iep.http.ServerRegistry;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.spectator.SpectatorEventsListenerFactory;

import javax.inject.Singleton;


public final class RxNettyModule extends AbstractModule {

  @Override protected void configure() {
    RxNetty.useMetricListenersFactory(new SpectatorEventsListenerFactory());
    bind(RxHttp.class).asEagerSingleton();
  }

  @Provides
  @Singleton
  private ServerRegistry providesServerRegistry(DiscoveryClient client) {
    return new EurekaServerRegistry(client);
  }

  @Override public boolean equals(Object obj) {
    return obj != null && getClass().equals(obj.getClass());
  }

  @Override public int hashCode() {
    return getClass().hashCode();
  }
}
