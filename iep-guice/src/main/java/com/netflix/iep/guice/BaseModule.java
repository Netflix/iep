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

import com.google.inject.AbstractModule;

import java.lang.reflect.Constructor;

/**
 * Base class for modules that provides some additional helper methods.
 */
public abstract class BaseModule extends AbstractModule {

  /**
   * Returns the only constructor for class {code}cls{code}. If the class has more than one
   * constructor, then an IllegalArgumentException will be thrown. This is typically used
   * for creating a quick constructor binding on a class that doesn't have an explicit
   * Inject annotation.
   */
  @SuppressWarnings("unchecked")
  protected <T> Constructor<T> getConstructor(Class<? extends T> cls) {
    Constructor<?>[] constructors = cls.getDeclaredConstructors();
    if (constructors.length != 1) {
      final String msg = "a single constructor is required, class " +
          cls.getName() + " has " + constructors.length + " constructors";
      throw new IllegalArgumentException(msg);
    }
    return (Constructor<T>) constructors[0];
  }
}
