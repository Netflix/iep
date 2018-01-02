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
package com.netflix.iep.admin;

import com.netflix.spectator.impl.Preconditions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wraps an arbitrary object and uses reflection to call methods like those specified
 * in {@link HttpEndpoint}. In other words it allows for duck typing of an endpoint. The
 * main use-case is to allow endpoints to be defined without having an explicit dependency
 * on the admin library.
 */
class BasicHttpEndpoint implements HttpEndpoint {

  private final Object obj;

  private final Method listMethod;
  private final Method getMethod;

  BasicHttpEndpoint(Object obj) {
    this.obj = Preconditions.checkNotNull(obj, "obj");
    listMethod = getMethod(obj, "get");
    getMethod = getMethod(obj, "get", String.class);
  }

  private Method getMethod(Object o, String name, Class<?>... params) {
    Class<?> cls = o.getClass();
    try {
      return cls.getMethod(name, params);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  private boolean isUserError(Throwable t) {
    return t instanceof IllegalArgumentException || t instanceof IllegalStateException;
  }

  @Override public Object get() {
    try {
      return (listMethod == null) ? null : listMethod.invoke(obj);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause != null)
        throw new HttpException(isUserError(cause) ? 400 : 500, cause);
      else
        throw new HttpException(500, e);
    } catch (IllegalAccessException e) {
      throw new HttpException(500, e);
    }
  }

  @Override public Object get(String path) {
    try {
      return (getMethod == null) ? null : getMethod.invoke(obj, path);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause != null)
        throw new HttpException(isUserError(cause) ? 400 : 500, cause);
      else
        throw new HttpException(500, e);
    } catch (IllegalAccessException e) {
      throw new HttpException(500, e);
    }
  }
}
