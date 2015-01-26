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
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import com.netflix.governator.lifecycle.LifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


/** Required javadoc for public class. */
public final class Governator {

  private static final Logger LOGGER = LoggerFactory.getLogger(Governator.class);

  private static final Governator INSTANCE = new Governator();

  public static Governator getInstance() {
    return INSTANCE;
  }

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

  private Governator() {
  }

  private Injector injector;

  public Injector getInjector() {
    return injector;
  }

  public void start(Iterable<Module> modules) throws Exception {
    LifecycleInjectorBuilder builder = LifecycleInjector.builder();
    injector = LifecycleInjector.builder()
        .withModules(modules)
        .build()
        .createInjector();

    LifecycleManager lcMgr = injector.getInstance(LifecycleManager.class);
    lcMgr.start();
  }

  public void shutdown() throws Exception {
    LifecycleManager lcMgr = injector.getInstance(LifecycleManager.class);
    lcMgr.close();
  }
}
