/*
 * Copyright 2014-2016 Netflix, Inc.
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
package com.netflix.iep.karyon3;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.TreeMap;


@Singleton
class EnvHandler extends SimpleHandler {

  @Inject
  EnvHandler(ObjectMapper mapper) {
    super(mapper);
  }

  @Override
  protected Object get() {
    // TreeMap is used for so user will conveniently get sorted keys, but is not required.
    Map<String, Object> wrapper = new TreeMap<>();
    wrapper.put("env", new TreeMap<>(System.getenv()));
    return wrapper;
  }
}
