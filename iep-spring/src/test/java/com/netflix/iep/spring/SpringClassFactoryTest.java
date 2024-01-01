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

import com.netflix.iep.service.ClassFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import jakarta.inject.Named;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@RunWith(JUnit4.class)
public class SpringClassFactoryTest {

  private AnnotationConfigApplicationContext createContext() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.registerBean(SpringClassFactory.class);
    return context;
  }

  @Test
  public void normal() throws Exception {
    try (AnnotationConfigApplicationContext context = createContext()) {
      context.registerBean(String.class, () -> "foo");
      context.registerBean(Normal.class);
      context.registerBean(TestClass.class);
      context.refresh();
      context.start();

      Normal obj = context.getBean(Normal.class);
      Assert.assertEquals("foo", obj.configClass.v);
    }
  }

  @Test
  public void override() throws Exception {
    try (AnnotationConfigApplicationContext context = createContext()) {
      context.registerBean(TestClass.class);
      context.registerBean(WithOverride.class);
      context.registerBean(String.class, () -> "foo");
      context.refresh();
      context.start();

      WithOverride obj = context.getBean(WithOverride.class);
      Assert.assertEquals("bar", obj.configClass.v);
    }
  }

  @Test
  public void supplier() throws Exception {
    try (AnnotationConfigApplicationContext context = createContext()) {
      context.register(SupplierClassConfiguration.class);
      context.registerBean(SupplierClass.class);
      context.registerBean(TestClass.class);
      context.registerBean(WithSupplier.class);
      context.registerBean(String.class, () -> "foo");
      context.refresh();
      context.start();

      WithSupplier obj = context.getBean(WithSupplier.class);
      Assert.assertEquals("foo", obj.configClass.v.get());
    }
  }

  @Test
  public void qualifers() throws Exception {
    try (AnnotationConfigApplicationContext context = createContext()) {
      context.register(StringConfiguration.class);
      context.registerBean(WithQualifier.class);
      context.registerBean(Wrapper.class);
      context.refresh();
      context.start();

      WithQualifier obj = context.getBean(WithQualifier.class);
      Assert.assertEquals("1", obj.s1);
      Assert.assertEquals("2", obj.s2);
    }
  }

  @Test
  public void directQualifier() throws Exception {
    try (AnnotationConfigApplicationContext context = createContext()) {
      context.register(StringConfiguration.class);
      context.registerBean(Wrapper.class);
      context.refresh();
      context.start();

      Wrapper obj = context.getBean(ClassFactory.class).newInstance(Wrapper.class);
      Assert.assertEquals("2", obj.s2);
    }
  }

  @Configuration
  public static class StringConfiguration {

    @Bean
    @Primary
    String one() {
      return "1";
    }

    @Bean
    @Named("s2")
    String two() {
      return "2";
    }
  }

  public static class Normal {
    final TestClass configClass;

    public Normal(ClassFactory factory) throws Exception {
      // Class name from configuration settings
      final String cname = TestClass.class.getName();
      configClass = factory.newInstance(cname);
    }
  }

  public static class WithOverride {
    final TestClass configClass;

    public WithOverride(ClassFactory factory) throws Exception {
      // Class name from configuration settings
      final String cname = TestClass.class.getName();
      final Map<Class<?>, Object> overrides = new HashMap<>();
      overrides.put(String.class, "bar");
      configClass = factory.newInstance(cname, overrides::get);
    }
  }

  public static class WithSupplier {
    final SupplierClass configClass;

    public WithSupplier(ClassFactory factory) throws Exception {
      // Class name from configuration settings
      final String cname = SupplierClass.class.getName();
      configClass = factory.newInstance(cname);
    }
  }

  public static class TestClass {
    final String v;

    public TestClass(String v) {
      this.v = v;
    }
  }

  @Configuration
  public static class SupplierClassConfiguration {

    @Bean
    Supplier<String> stringValue(ApplicationContext context) {
      return () -> context.getBean(String.class);
    }
  }

  public static class SupplierClass {
    final Supplier<String> v;

    public SupplierClass(Supplier<String> v) {
      this.v = v;
    }
  }

  public static class WithQualifier {
    final String s1;
    final String s2;

    public WithQualifier(String s1, Wrapper wrapper) {
      this.s1 = s1;
      this.s2 = wrapper.s2;
    }
  }

  public static class Wrapper {
    final String s2;

    public Wrapper(@Named("s2") String s2) {
      this.s2 = s2;
    }
  }
}
