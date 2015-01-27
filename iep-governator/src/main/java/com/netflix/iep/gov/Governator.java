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
package com.netflix.iep.gov;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import com.netflix.governator.lifecycle.LifecycleManager;
import org.apache.commons.configuration.EnvironmentConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;


/** Required javadoc for public class. */
public final class Governator {

  private static final Logger LOGGER = LoggerFactory.getLogger(Governator.class);

  private static final String SERVICE_LOADER = "service-loader";
  private static final String NONE = "none";

  private static final String ARCHAIUS_CONFIG_FILE = "platformservice";

  private static final Governator INSTANCE = new Governator();

  public static Governator getInstance() {
    return INSTANCE;
  }

  /** Add a set of tasks to be executed after shutting down governator. */
  public static void addShutdownHook(final Runnable... tasks) {
    final Runnable r = new Runnable() {
      @Override public void run() {
        try {
          getInstance().shutdown();
          for (Runnable task : tasks) {
            task.run();
          }
        } catch (Exception e) {
          LOGGER.warn("exception during shutdown sequence", e);
        }
      }
    };
    Runtime.getRuntime().addShutdownHook(new Thread(r, "ShutdownHook"));
  }

  /**
   * Returns a list of all guice modules in the classpath using ServiceLoader. Modules that do
   * not have a corresponding provider config will not get loaded.
   */
  public static List<Module> getModulesUsingServiceLoader() {
    ServiceLoader<Module> loader = ServiceLoader.load(Module.class);
    List<Module> modules = new ArrayList<>();
    for (Module m : loader) {
      modules.add(m);
    }
    return modules;
  }

  /**
   * Return a list of all modules based on the value of property {@code k}. The value should be
   * a comma separated list of classes that implement {@link com.google.inject.Module}. If the
   * property is not set or uses the value {@code service-loader} then it will add in all the
   * modules using the java ServiceLoader utility. The value {@code none} can be used to indicate
   * an empty list with no modules.
   */
  public static List<Module> getModulesUsingProp(String k) throws Exception {
    List<Module> modules = new ArrayList<>();
    List<Object> vs = ConfigurationManager.getConfigInstance()
        .getList(k, Collections.singletonList(SERVICE_LOADER));
    for (Object v : vs) {
      String cname = (String) v;
      if (SERVICE_LOADER.equals(cname)) {
        modules.addAll(getModulesUsingServiceLoader());
      } else if (!cname.isEmpty() && !NONE.equals(cname)) {
        modules.add((Module) Class.forName(cname).newInstance());
      }
    }
    return modules;
  }

  /** Get modules specified with the system prop {@code netflix.iep.gov.modules}. */
  public static List<Module> getModules() throws Exception {
    return getModulesUsingProp("netflix.iep.gov.modules");
  }

  public static void loadProperties(String name) {
    try {
      ConfigurationManager.loadCascadedPropertiesFromResources(name);
    } catch (IOException e) {
      LOGGER.warn("failed to load properties for '" + name + "'");
    }
  }

  private Governator() {
    initArchaius();
  }

  private Injector injector;

  /** Return the injector used with the governator lifecycle. */
  public Injector getInjector() {
    return injector;
  }

  /** Start up governator using the list of modules from {@link #getModules()}. */
  public void start() throws Exception {
    start(getModules());
  }

  /** Start up governator with an arbitrary list of modules. */
  public void start(Iterable<Module> modules) throws Exception {
    LifecycleInjectorBuilder builder = LifecycleInjector.builder();
    injector = LifecycleInjector.builder()
        .withModules(modules)
        .build()
        .createInjector();

    LifecycleManager lcMgr = injector.getInstance(LifecycleManager.class);
    lcMgr.start();
  }

  private void initArchaius() {
    ConcurrentCompositeConfiguration composite = new ConcurrentCompositeConfiguration();
    composite.addConfiguration(new SystemConfiguration(), "system");
    composite.addConfiguration(new EnvironmentConfiguration(), "environment");
    ConfigurationManager.install(composite);
    loadProperties(ARCHAIUS_CONFIG_FILE);
    loadProperties("application");
  }

  /** Shutdown governator. */
  public void shutdown() throws Exception {
    LifecycleManager lcMgr = injector.getInstance(LifecycleManager.class);
    lcMgr.close();
  }
}
