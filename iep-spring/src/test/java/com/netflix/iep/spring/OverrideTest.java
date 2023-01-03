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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@RunWith(JUnit4.class)
public class OverrideTest {

  private AnnotationConfigApplicationContext createContext() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.setAllowBeanDefinitionOverriding(false);
    return context;
  }

  @Test(expected = BeanDefinitionOverrideException.class)
  public void dedup() throws Exception {
    try (AnnotationConfigApplicationContext context = createContext()) {
      context.register(ConfigA.class, ConfigA.class);
      context.refresh();

      context.start();
      Assert.assertEquals("A", context.getBean(String.class));
    }
  }

  @Test(expected = BeanDefinitionOverrideException.class)
  public void conflict() throws Exception {
    try (AnnotationConfigApplicationContext context = createContext()) {
      context.register(ConfigA.class, ConfigB.class);
      context.refresh();

      context.start();
      Assert.assertEquals("A", context.getBean(String.class));
    }
  }

  @Test
  public void override() throws Exception {
    try (AnnotationConfigApplicationContext context = createContext()) {
      context.register(ConfigC.class);
      context.refresh();

      context.start();
      Assert.assertEquals("C", context.getBean(String.class));
    }
  }

  @Test(expected = BeanDefinitionOverrideException.class)
  public void dedupWithOverride() throws Exception {
    try (AnnotationConfigApplicationContext context = createContext()) {
      context.register(ConfigA.class, ConfigC.class);
      context.refresh();

      context.start();
      Assert.assertEquals("C", context.getBean(String.class));
    }
  }

  @Configuration
  public static class ConfigA {

    @Bean
    String stringValue() {
      return "A";
    }
  }

  @Configuration
  public static class ConfigB {

    @Bean
    String stringValue() {
      return "B";
    }
  }

  @Configuration
  public static class ConfigC {

    @Bean
    @Primary
    String stringValue() {
      return "C";
    }
  }
}
