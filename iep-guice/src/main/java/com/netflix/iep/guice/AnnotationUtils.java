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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Helper functions for checking the annotations on an object.
 */
class AnnotationUtils {

  private AnnotationUtils() {
  }

  static boolean hasLifecycleAnnotations(Class<?> cls) throws Exception {
    return getPostConstruct(cls) != null || getPreDestroy(cls) != null;
  }

  static Method getPostConstruct(Class<?> cls) throws Exception {
    return getAnnotatedMethod(cls, PostConstruct.class);
  }

  static Method getPreDestroy(Class<?> cls) throws Exception {
    return getAnnotatedMethod(cls, PreDestroy.class);
  }

  static Method getAnnotatedMethod(Class<?> cls, Class<? extends Annotation> anno)
      throws Exception {
    for (Method m : cls.getDeclaredMethods()) {
      if (m.getAnnotation(anno) != null) {
        return m;
      }
    }
    Class<?> superCls = cls.getSuperclass();
    return (superCls != null) ? getAnnotatedMethod(superCls, anno) : null;
  }
}
