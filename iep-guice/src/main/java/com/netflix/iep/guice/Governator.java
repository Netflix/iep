/*
 * Copyright 2015 Netflix, Inc.
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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/** Helper for using guice with some basic lifecycle. */
public final class Governator {

  private static final Logger LOGGER = LoggerFactory.getLogger(Governator.class);

  private static final Governator INSTANCE = new Governator();

  public static Governator getInstance() {
    return INSTANCE;
  }

  /** Add a task to be executed after shutting down governator. */
  public static void addShutdownHook() {
    final Runnable r = new Runnable() {
      @Override public void run() {
        try {
          getInstance().shutdown();
        } catch (Exception e) {
          LOGGER.warn("exception during shutdown sequence", e);
        }
      }
    };
    Runtime.getRuntime().addShutdownHook(new Thread(r, "ShutdownHook"));
  }

  /**
   * Returns a list of all guice modules in the classpath using ServiceLoader. Modules that do
   * not have a corresponding provider config.properties will not get loaded.
   */
  public static List<Module> getModulesUsingServiceLoader() {
    ServiceLoader<Module> loader = ServiceLoader.load(Module.class);
    List<Module> modules = new ArrayList<>();
    for (Module m : loader) {
      modules.add(m);
    }
    return modules;
  }

  private Governator() {
  }

  private Injector injector;

  /** Return the injector used with the governator lifecycle. */
  public Injector getInjector() {
    return injector;
  }

  /** Start up governator using the list of modules from {@link #getModulesUsingServiceLoader()}. */
  public void start() throws Exception {
    start(getModulesUsingServiceLoader());
  }

  /** Start up governator with an arbitrary list of modules. */
  public void start(Iterable<Module> modules) throws Exception {
    List<Module> ms = new ArrayList<>();
    ms.add(new LifecycleModule());
    for (Module m : modules) {
      LOGGER.debug("adding module: {}", m.getClass());
      ms.add(m);
    }
    injector = Guice.createInjector(ms);
    addShutdownHook();
  }

  /** Shutdown governator. */
  public void shutdown() throws Exception {
    PreDestroyList list = injector.getInstance(PreDestroyList.class);
    list.invokeAll();
  }
}
