/*
 * Copyright 2014-2019 Netflix, Inc.
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

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.netflix.iep.leader.api.LeaderElector;
import com.netflix.iep.leader.api.LeaderStatus;
import com.netflix.iep.service.Service;

public class LeaderModule extends AbstractModule {

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), Service.class).addBinding().to(LeaderService.class);

    bind(LeaderElector.class).to(StandardLeaderElector.class);
    bind(LeaderStatus.class).to(StandardLeaderElector.class);
  }
}
