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
package com.netflix.iep.guice;

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.netflix.iep.service.ClassFactory;
import com.netflix.iep.service.DefaultClassFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
class ClassFactoryProvider implements Provider<ClassFactory> {

  private final Injector injector;

  @Inject
  ClassFactoryProvider(Injector injector) {
    this.injector = injector;
  }

  @Override
  public ClassFactory get() {
    return new DefaultClassFactory(type -> {
      try {
        return injector.getInstance(Key.get(type));
      } catch (ConfigurationException e) {
        return null;
      }
    });
  }
}
