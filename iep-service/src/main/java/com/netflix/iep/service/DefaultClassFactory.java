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
package com.netflix.iep.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.function.Function;

/**
 * Simple implementation of ClassInstanceFactory that works for classes that have a no-argument
 * constructor or a single constructor with explicit bindings provided.
 */
public class DefaultClassFactory implements ClassFactory {

  private final Function<Type, Object> bindings;

  public DefaultClassFactory() {
    this(c -> null);
  }

  public DefaultClassFactory(Function<Type, Object> bindings) {
    this.bindings = bindings;
  }

  @SuppressWarnings("unchecked")
  @Override public <T> T newInstance(Type type, Function<Type, Object> overrides)
      throws CreationException {
    Class<?> cls = (Class<?>) type;
    Constructor<?>[] constructors = cls.getDeclaredConstructors();
    if (constructors.length == 1) {
      Constructor<?> c = constructors[0];
      return newInstance(cls, c, c.getGenericParameterTypes(), overrides);
    } else {
      for (Constructor<?> c : constructors) {
        Type[] ptypes = c.getGenericParameterTypes();
        if (ptypes.length == 0) {
          return newInstance(cls, c, ptypes, overrides);
        }
      }
      throw new CreationException("class " + cls.getCanonicalName() +
          " has more than one constructor and does not have a no-argument constructor");
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T newInstance(
      Type type,
      Constructor<?> c,
      Type[] ptypes,
      Function<Type, Object> overrides) throws CreationException {
    try {
      c.setAccessible(true);
      if (ptypes.length == 0) {
        return (T) c.newInstance();
      } else {
        Object[] pvalues = new Object[ptypes.length];
        for (int i = 0; i < ptypes.length; ++i) {
          pvalues[i] = overrides.apply(ptypes[i]);
          if (pvalues[i] == null) {
            pvalues[i] = bindings.apply(ptypes[i]);
          }
        }

        return (T) c.newInstance(pvalues);
      }
    } catch (Exception e) {
      throw new CreationException(type, e);
    }
  }
}
