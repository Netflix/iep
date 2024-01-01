/*
 * Copyright 2014-2024 Netflix, Inc.
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
package com.netflix.iep.spring;

import com.netflix.iep.service.ServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * General purpose main class for standalone applications based on Spring ApplicationContext and
 * {@link com.netflix.iep.service.ServiceManager}.
 */
public class Main implements AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  private static final String SCAN_PACKAGES_PROP = "netflix.iep.spring.scanPackages";

  private static final String EXIT_ON_FAILURE_PROP = "netflix.iep.spring.exitOnFailure";

  private static String[] scanPackages() {
    String pkgs = System.getProperty(SCAN_PACKAGES_PROP, "com.netflix");
    return pkgs.isEmpty() ? null : pkgs.split(",");
  }

  private static boolean exitOnFailure() {
    return "true".equals(System.getProperty(EXIT_ON_FAILURE_PROP, "true"));
  }

  private final AnnotationConfigApplicationContext context;

  Main(AnnotationConfigApplicationContext context) {
    this.context = context;
  }

  public ApplicationContext getContext() {
    return context;
  }

  void runImpl(String[] args) throws Exception {
    // Send uncaught exceptions to the expected logger. Register early as errors
    // could get generated during startup.
    Thread.setDefaultUncaughtExceptionHandler((thread, e) ->
      LOGGER.error("Uncaught exception from thread {} ({})", thread.getName(), thread.getId(), e)
    );

    try {
      // Binding for command line arguments
      context.registerBean(Args.class, () -> Args.from(args));
      context.refresh();

      // Start up
      context.start();
      context.registerShutdownHook();

      // Make sure service manager is created
      context.getBean(ServiceManager.class);
      LOGGER.info("service started successfully");
    } catch (Throwable t) {
      LOGGER.error("service failed, shutting down", t);
      if (exitOnFailure()) {
        System.exit(1);
      }
      throw t;
    }
  }

  @Override
  public void close() throws Exception {
    context.close();
  }

  public static Main run(String[] args, AnnotationConfigApplicationContext context) throws Exception {
    Main m = new Main(context);
    m.runImpl(args);
    return m;
  }

  public static Main run(String[] args) throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    String[] pkgs = scanPackages();
    if (pkgs != null) {
      context.scan(pkgs);
    }
    context.setAllowBeanDefinitionOverriding(false);
    return run(args, context);
  }

  public static void main(String[] args) throws Exception {
    run(args);
  }
}
