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
package com.netflix.iep.config;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Configuration {
  private final static Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

  private static volatile IConfiguration iConfiguration = key -> {
    throw new IllegalStateException("configuration has not been set");
  };

  static void setConfiguration(IConfiguration config) {
    iConfiguration = config;
  }

  public static <T> T apply(Class<T> ctype) {
    String pkg = ctype.getPackage().getName();
    String prefix = (pkg.startsWith("com.")) ? pkg.substring("com.".length()) : pkg;
    return newProxy(ctype, prefix);
  }

  @SuppressWarnings("unchecked")
  public static <T> T newProxy(
    final Class<T> ctype,
    final String prefix
   ) {
    InvocationHandler handler = (proxy, method, args) -> {
      if (method.getName().equals("get")) {
        return iConfiguration.get((args[0] == null) ? null : args[0].toString());
      }
      else {
        Class rt = method.getReturnType();
        String key = (prefix == null) ? method.getName() : prefix + "." + method.getName();
        if (IConfiguration.class.isAssignableFrom(rt)) {
          return newProxy(rt, key);
        }
        else {
          String value = iConfiguration.get(key);
          if (value == null) {
            DefaultValue anno = method.getAnnotation(DefaultValue.class);
            value = (anno == null) ? null : anno.value();
          }
          if (value == null) {
            if (rt.isPrimitive())
              throw new IllegalStateException("no value for property " + method.getName());
             return null;
          }
          return Strings.cast(rt, value);
        }
      }
    };
    return (T) Proxy.newProxyInstance(ctype.getClassLoader(), new Class[]{ctype}, handler);
  }
}
