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

import java.lang.reflect.Type;
import java.util.function.Function;

/**
 * Utility for creating an instance of a class based on the name. Typically used to create
 * an instance based on a class name in a config file. This will handle simple constructor
 * injection cases based on the parameter types. If more fine-grained control is needed, then
 * use a proper injection framework directly.
 */
public interface ClassFactory {

  /**
   * Create a new instance of a class with the provided name.
   *
   * @param name
   *     Name of the class that can be used with calls such as {@link Class#forName(String)}.
   * @return
   *     New instance of the class. Note, a new instance will be created for every call.
   */
  default <T> T newInstance(String name) throws ClassNotFoundException {
    return newInstance(name, c -> null);
  }

  /**
   * Create a new instance of a class with the provided name.
   *
   * @param name
   *     Name of the class that can be used with calls such as {@link Class#forName(String)}.
   * @param overrides
   *     Override bindings to use for the constructor parameter types.
   * @return
   *     New instance of the class. Note, a new instance will be created for every call.
   */
  @SuppressWarnings("unchecked")
  default <T> T newInstance(String name, Function<Type, Object> overrides)
      throws ClassNotFoundException {
    final Class<T> cls = (Class<T>) Class.forName(name);
    return newInstance(cls, overrides);
  }

  /**
   * Create a new instance of a class with the provided name.
   *
   * @param cls
   *     Class to use for creating a new instance.
   * @return
   *     New instance of the class. Note, a new instance will be created for every call.
   */
  default <T> T newInstance(Type cls) {
    return newInstance(cls, c -> null);
  }

  /**
   * Create a new instance of a class with the provided name. Overrides can be used to
   * provide some local bindings for creating that instance. For example suppose we have a
   * config block associated with the class name.
   *
   * Consider a simple server class:
   *
   * <pre>
   * public class Server {
   *   private final int port;
   *
   *   public Server(Config cfg) {
   *     port = cfg.getInt("port");
   *   }
   *   ...
   * }
   * </pre>
   *
   * Possible configuration using HOCON:
   *
   * <pre>
   * server.connectors = [
   *   {
   *     class = "foo.Server"
   *     port = 8080
   *   },
   *   {
   *     class = "foo.Server"
   *     port = 8081
   *   }
   * ]
   * </pre>
   *
   * This could be loaded like:
   *
   * <pre>
   * ClassFactory factory = ...
   * for (Config cfg : config.getConfigList("server.connectors")) {
   *   Map<Class<?>, Object> overrides = Collections.singletonMap(Config.class, cfg);
   *   servers.add(factory.newInstance(cfg.getString("class"), overrides));
   * }
   * </pre>
   *
   * @param cls
   *     Class to use for creating a new instance.
   * @param overrides
   *     Override bindings to use for the constructor parameter types.
   * @return
   *     New instance of the class. Note, a new instance will be created for every call.
   */
  <T> T newInstance(Type cls, Function<Type, Object> overrides);
}
