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

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Helper for listening to injection events and invoking the PostConstruct and PreDestroy
 * annotated methods.
 */
public class LifecycleModule extends AbstractModule {

  private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleModule.class);

  private static class LifecycleListener implements InjectionListener<Object> {

    private final PreDestroyList preDestroyList;

    LifecycleListener(PreDestroyList preDestroyList) {
      this.preDestroyList = preDestroyList;
    }

    @Override public void afterInjection(Object injectee) {
      try {
        Method postConstruct = AnnotationUtils.getPostConstruct(injectee.getClass());
        if (postConstruct != null) {
          LOGGER.debug("invoking @PostConstruct for {}", injectee.getClass().getName());
          try {
            postConstruct.setAccessible(true);
            postConstruct.invoke(injectee);
          } catch (Throwable t) {
            LOGGER.debug("error calling @PostConstruct (" + postConstruct + ")", t);
            throw t;
          }
          LOGGER.debug("completed @PostConstruct ({})", postConstruct);
        }

        Method preDestroy = AnnotationUtils.getPreDestroy(injectee.getClass());
        if (preDestroy != null) {
          preDestroyList.add(injectee);
        }
      } catch (Exception e) {
        throw new RuntimeException("", e);
      }
    }
  };

  private static class BindingListener implements TypeListener {

    private final LifecycleListener lifecycle;

    BindingListener(LifecycleListener lifecycle) {
      this.lifecycle = lifecycle;
    }

    @Override public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
      try {
        if (AnnotationUtils.hasLifecycleAnnotations(type.getRawType())) {
          encounter.register(lifecycle);
        }
      } catch (Exception e) {
        LOGGER.warn("failed to check annotations on " + type.getRawType(), e);
      }
    }
  };

  @Override protected void configure() {
    PreDestroyList list = new PreDestroyList();
    LifecycleListener lifecycle = new LifecycleListener(list);
    bindListener(Matchers.any(), new BindingListener(lifecycle));
    bind(PreDestroyList.class).toInstance(list);
  }

  @Override public boolean equals(Object obj) {
    return obj != null && getClass().equals(obj.getClass());
  }

  @Override public int hashCode() {
    return getClass().hashCode();
  }
}
