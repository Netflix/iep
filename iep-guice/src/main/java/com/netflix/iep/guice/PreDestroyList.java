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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.lang.reflect.Method;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Objects with a PreDestroy annotation that need to be cleaned up with the application.
 */
@Singleton
public class PreDestroyList {

  private static final Logger LOGGER = LoggerFactory.getLogger(PreDestroyList.class);

  private final CopyOnWriteArrayList<Object> cleanupList;

  PreDestroyList() {
    this.cleanupList = new CopyOnWriteArrayList<>();
  }

  void add(Object obj) {
    LOGGER.debug("registering class with cleanup manager {}", obj.getClass().getName());
    cleanupList.add(obj);
  }

  public void invokeAll() throws Exception {
    int n = cleanupList.size();
    for (int i = n - 1; i >= 0; --i) {
      Object obj = cleanupList.get(i);
      if (obj instanceof AutoCloseable) {
        LOGGER.debug("invoking close() for {}", obj.getClass().getName());
        ((AutoCloseable) obj).close();
      }
      Method preDestroy = AnnotationUtils.getPreDestroy(obj.getClass());
      if (preDestroy != null) {
        LOGGER.debug("invoking @PreDestroy for {}", obj.getClass().getName());
        preDestroy.setAccessible(true);
        preDestroy.invoke(obj);
      }
    }
  }
}
