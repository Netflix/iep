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

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.netflix.iep.service.ClassFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.HashMap;
import java.util.Map;

@RunWith(JUnit4.class)
public class GuiceClassFactoryTest {

  @Test
  public void normal() throws Exception {
    Module module = new AbstractModule() {
      @Override protected void configure() {
        bind(String.class).toInstance("foo");
      }
    };
    GuiceHelper helper = new GuiceHelper();
    helper.start(module);

    Normal obj = helper.getInjector().getInstance(Normal.class);
    Assert.assertEquals("foo", obj.configClass.v);

    helper.shutdown();
  }

  @Test
  public void override() throws Exception {
    Module module = new AbstractModule() {
      @Override protected void configure() {
        bind(String.class).toInstance("foo");
      }
    };
    GuiceHelper helper = new GuiceHelper();
    helper.start(module);

    WithOverride obj = helper.getInjector().getInstance(WithOverride.class);
    Assert.assertEquals("bar", obj.configClass.v);

    helper.shutdown();
  }

  @Test
  public void provider() throws Exception {
    Module module = new AbstractModule() {
      @Override protected void configure() {
        bind(String.class).toInstance("foo");
      }
    };
    GuiceHelper helper = new GuiceHelper();
    helper.start(module);

    WithProvider obj = helper.getInjector().getInstance(WithProvider.class);
    Assert.assertEquals("foo", obj.configClass.v.get());

    helper.shutdown();
  }

  public static class Normal {
    final TestClass configClass;

    @Inject
    public Normal(ClassFactory factory) throws Exception {
      // Class name from configuration settings
      final String cname = TestClass.class.getName();
      configClass = factory.newInstance(cname);
    }
  }

  public static class WithOverride {
    final TestClass configClass;

    @Inject
    public WithOverride(ClassFactory factory) throws Exception {
      // Class name from configuration settings
      final String cname = TestClass.class.getName();
      final Map<Class<?>, Object> overrides = new HashMap<>();
      overrides.put(String.class, "bar");
      configClass = factory.newInstance(cname, overrides::get);
    }
  }

  public static class WithProvider {
    final ProviderClass configClass;

    @Inject
    public WithProvider(ClassFactory factory) throws Exception {
      // Class name from configuration settings
      final String cname = ProviderClass.class.getName();
      configClass = factory.newInstance(cname);
    }
  }

  public static class TestClass {
    final String v;

    @Inject
    public TestClass(String v) {
      this.v = v;
    }
  }

  public static class ProviderClass {
    final Provider<String> v;

    @Inject
    public ProviderClass(Provider<String> v) {
      this.v = v;
    }
  }
}
