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
package com.netflix.iep.admin.guice;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.netflix.iep.admin.HttpEndpoint;
import com.netflix.iep.admin.endpoints.ReflectEndpoint;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Endpoint that allows the standard reflection endpoint to work after first looking up a
 * key in the injector. Provides a simple way to do simple inspection of fields for objects
 * available via the injector.
 */
@Singleton
public class GuiceDebugEndpoint implements HttpEndpoint {

  private final Injector injector;
  private final GuiceEndpoint guiceEndpoint;

  @Inject
  public GuiceDebugEndpoint(Injector injector) {
    this.injector = injector;
    this.guiceEndpoint = new GuiceEndpoint(injector);
  }

  @Override public Object get() {
    return guiceEndpoint.getKeySet(v -> true);
  }

  @Override public Object get(String path) {
    int i = path.indexOf('/');
    if (i < 0) {
      ReflectEndpoint endpoint = getEndpoint(path);
      return (endpoint == null) ? null : endpoint.get();
    } else {
      ReflectEndpoint endpoint = getEndpoint(path.substring(0, i));
      return (endpoint == null) ? null : endpoint.get(path.substring(i + 1));
    }
  }

  private ReflectEndpoint getEndpoint(String k) {
    Key<?> key = guiceEndpoint.getBindingKeys(v -> true).get(k);
    return (key == null) ? null : new ReflectEndpoint(injector.getInstance(key));
  }
}
