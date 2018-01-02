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
package com.netflix.iep.eureka;

import com.netflix.iep.service.AbstractService;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Reports as unhealthy after stop is called. For clients that use discovery to find the node
 * this ensures that the status will show as down and traffic can move off.
 */
@Singleton
class EurekaService extends AbstractService {

  @Inject
  EurekaService() {
    super();
  }

  @Override protected void startImpl() throws Exception {
  }

  @Override protected void stopImpl() throws Exception {
  }
}
