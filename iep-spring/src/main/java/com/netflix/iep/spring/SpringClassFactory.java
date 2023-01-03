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

import com.netflix.iep.service.DefaultClassFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.ParameterizedType;

@Singleton
class SpringClassFactory extends DefaultClassFactory {

  @Inject
  SpringClassFactory(ApplicationContext context) {
    super(type -> {
      try {
        if (type instanceof Class<?>) {
          return context.getBean((Class<?>) type);
        } else if (type instanceof ParameterizedType) {
          ResolvableType rt = ResolvableType.forType(type);
          String[] beanNames = context.getBeanNamesForType(rt);
          return beanNames.length == 0
              ? null
              : context.getBean(beanNames[0]);
        } else {
          return null;
        }
      } catch (NoSuchBeanDefinitionException e) {
        return null;
      }
    });
  }
}
