/*
 * Copyright 2014-2023 Netflix, Inc.
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

import com.netflix.iep.service.ClassFactory;
import com.netflix.iep.service.CreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.function.Function;

class SpringClassFactory implements ClassFactory {

  private final ApplicationContext context;
  private final DefaultListableBeanFactory factory;

  SpringClassFactory(ApplicationContext context) {
    this.context = context;
    BeanFactory bf = context.getAutowireCapableBeanFactory();
    this.factory = (bf instanceof DefaultListableBeanFactory)
        ? (DefaultListableBeanFactory) bf
        : null;
  }

  @Override public <T> T newInstance(Type type, Function<Type, Object> overrides)
      throws CreationException {
    Class<?> cls = (Class<?>) type;
    Constructor<?>[] constructors = cls.getDeclaredConstructors();
    if (constructors.length == 1) {
      Constructor<?> c = constructors[0];
      Type[] ptypes = c.getGenericParameterTypes();
      Object[] pvalues = new Object[ptypes.length];
      for (int i = 0; i < pvalues.length; ++i) {
        Object value = overrides.apply(ptypes[i]);
        if (value == null) {
          MethodParameter parameter = new MethodParameter(c, i);
          value = getBeanForType(parameter);
        }
        pvalues[i] = value;
      }
      return newInstance(cls, c, pvalues);
    } else {
      for (Constructor<?> c : constructors) {
        Type[] ptypes = c.getGenericParameterTypes();
        if (ptypes.length == 0) {
          return newInstance(cls, c);
        }
      }
      throw new CreationException("class " + cls.getCanonicalName() +
          " has more than one constructor and does not have a no-argument constructor");
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T newInstance(Type type, Constructor<?> ctor, Object... values)
      throws CreationException {
    try {
      ctor.setAccessible(true);
      return (T) ctor.newInstance(values);
    } catch (Exception e) {
      throw new CreationException(type, e);
    }
  }

  private Object getBeanForType(MethodParameter parameter) {
    try {
      DependencyDescriptor descriptor = new DependencyDescriptor(parameter, true, false);
      if (factory != null) {
        // Needed to resolve qualifiers
        return factory.resolveDependency(descriptor, null);
      } else {
        // If factory type is wrong, then just try based on type
        return context.getBean(parameter.getParameterType());
      }
    } catch (NoSuchBeanDefinitionException e) {
      return null;
    }
  }
}
