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
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import com.netflix.iep.service.Service;
import com.netflix.iep.service.ServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * General purpose main class for standalone applications based on {@link GuiceHelper} and
 * {@link com.netflix.iep.service.ServiceManager}.
 */
public class Main {

  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  private static final String USE_SERVICE_LOADER_PROP = "netflix.iep.guice.useServiceLoader";

  private static final String EXIT_ON_FAILURE_PROP = "netflix.iep.guice.exitOnFailure";

  private static final String MODULES_PROP = "netflix.iep.guice.modules";

  private static boolean useServiceLoader() {
    return "true".equals(System.getProperty(USE_SERVICE_LOADER_PROP, "true"));
  }

  private static boolean exitOnFailure() {
    return "true".equals(System.getProperty(EXIT_ON_FAILURE_PROP, "true"));
  }

  private static List<Module> loadExplicitModules() throws Exception {
    List<Module> modules = new ArrayList<>();
    String[] cnames = System.getProperty(MODULES_PROP, "").split("[,\\s]+");
    for (String cname : cnames) {
      if (!"".equals(cname)) {
        Class<?> cls = Class.forName(cname);
        modules.add((Module) cls.newInstance());
      }
    }
    return modules;
  }

  private GuiceHelper helper;

  Main() {
  }

  GuiceHelper getHelper() {
    return helper;
  }

  void run(String[] args) throws Exception {
    try {
      // Setup binding for command line arguments
      Module argsModule = new AbstractModule() {
        @Override protected void configure() {
          Multibinder.newSetBinder(binder(), Service.class);
          bind(Args.class).toInstance(Args.from(args));
        }
      };

      // Load modules present in the classpath
      List<Module> modules = useServiceLoader()
          ? GuiceHelper.getModulesUsingServiceLoader()
          : new ArrayList<>();
      modules.addAll(loadExplicitModules());
      modules.add(argsModule);

      // Create injector and start up
      helper = new GuiceHelper();
      helper.start(modules);
      helper.addShutdownHook();

      // Make sure service manager is created
      helper.getInjector().getInstance(ServiceManager.class);
      LOGGER.info("service started successfully");
    } catch (Throwable t) {
      LOGGER.error("service failed, shutting down", t);
      if (exitOnFailure()) {
        System.exit(1);
      }
      throw t;
    }
  }

  public static void main(String[] args) throws Exception {
    (new Main()).run(args);
  }
}
