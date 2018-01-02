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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Helper functions for checking the annotations on an object.
 */
class AnnotationUtils {

  private AnnotationUtils() {
  }

  // On JDK9 and later these annotations may not be available, so we do not want to
  // reference the annotation classes directly
  private static final Class<? extends Annotation> POST_CONSTRUCT =
      getAnnotationClass("javax.annotation.PostConstruct");
  private static final Class<? extends Annotation> PRE_DESTROY =
      getAnnotationClass("javax.annotation.PreDestroy");

  @SuppressWarnings("unchecked")
  private static Class<? extends Annotation> getAnnotationClass(String name) {
    try {
      return (Class<? extends Annotation>) Class.forName(name);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  static boolean hasLifecycleAnnotations(Class<?> cls) throws Exception {
    return getPostConstruct(cls) != null || getPreDestroy(cls) != null;
  }

  static Method getPostConstruct(Class<?> cls) throws Exception {
    return getAnnotatedMethod(cls, POST_CONSTRUCT);
  }

  static Method getPreDestroy(Class<?> cls) throws Exception {
    return getAnnotatedMethod(cls, PRE_DESTROY);
  }

  static Method getAnnotatedMethod(Class<?> cls, Class<? extends Annotation> anno) {
    if (anno == null) {
      return null;
    }
    for (Method m : cls.getDeclaredMethods()) {
      if (m.getAnnotation(anno) != null) {
        return m;
      }
    }
    Class<?> superCls = cls.getSuperclass();
    return (superCls != null) ? getAnnotatedMethod(superCls, anno) : null;
  }

  static void invokePostConstruct(Logger logger, Object injectee, PreDestroyList preDestroyList) {
    try {
      // Invoke initialization callback if present
      Method postConstruct = AnnotationUtils.getPostConstruct(injectee.getClass());
      if (postConstruct != null) {
        logger.debug("invoking @PostConstruct for {}", injectee.getClass().getName());
        try {
          postConstruct.setAccessible(true);
          postConstruct.invoke(injectee);
        } catch (Throwable t) {
          logger.debug("error calling @PostConstruct (" + postConstruct + ")", t);
          throw t;
        }
        logger.debug("completed @PostConstruct ({})", postConstruct);
      }

      // Add object to list to destroy if there is a shutdown callback. If the pre destroy
      // list is null that means the injectee is not a singleton and thus the lifecycle should
      // be managed by the user not tied to the injector.
      if (preDestroyList != null) {
        if (injectee instanceof AutoCloseable) {
          preDestroyList.add(injectee);
        } else {
          Method preDestroy = AnnotationUtils.getPreDestroy(injectee.getClass());
          if (preDestroy != null) {
            preDestroyList.add(injectee);
          }
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
