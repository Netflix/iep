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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;

/** Helper for using guice with some basic lifecycle. */
public final class GuiceHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(GuiceHelper.class);

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

  private Injector injector;

  public GuiceHelper() {
  }

  /** Return the injector used with the lifecycle. */
  public Injector getInjector() {
    return injector;
  }

  /** Start up using the list of modules from {@link #getModulesUsingServiceLoader()}. */
  public void start() throws Exception {
    start(getModulesUsingServiceLoader());
  }

  /** Start up with an arbitrary list of modules. */
  public void start(Module... modules) throws Exception {
    start(Arrays.asList(modules));
  }

  /** Start up with an arbitrary list of modules. */
  public void start(Iterable<Module> modules) throws Exception {
    List<Module> ms = new ArrayList<>();
    ms.add(new LifecycleModule());
    for (Module m : modules) {
      LOGGER.debug("adding module: {}", m.getClass());
      ms.add(m);
    }
    injector = Guice.createInjector(ms);
    LOGGER.info("guice injector created successfully");
  }

  /** Shutdown classes with {@link javax.annotation.PreDestroy}. */
  public void shutdown() throws Exception {
    PreDestroyList list = injector.getInstance(PreDestroyList.class);
    list.invokeAll();
  }

  /** Add a shutdown hook for this instance. */
  public void addShutdownHook() {
    final Runnable r = () -> {
      try {
        shutdown();
        LOGGER.info("shutdown completed successfully");
      } catch (Exception e) {
        LOGGER.warn("exception during shutdown sequence", e);
      }
    };
    Runtime.getRuntime().addShutdownHook(new Thread(r, "ShutdownHook"));
  }
}
