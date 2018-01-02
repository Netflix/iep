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

import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public class BaseModuleTest {

  @Test(expected = CreationException.class)
  public void noInjectBinding() throws Exception {
    Guice.createInjector(new BaseModule() {
      @Override protected void configure() {
        bind(String.class).toInstance("foo");
        bind(Foo.class).to(Foo.class);
      }
    });
  }

  @Test
  public void constructorBinding() throws Exception {
    Injector injector = Guice.createInjector(new BaseModule() {
      @Override protected void configure() {
        bind(String.class).toInstance("foo");
        bind(Foo.class).toConstructor(getConstructor(Foo.class));
      }
    });
    Assert.assertEquals("foo", injector.getInstance(Foo.class).value);
  }

  @Test
  public void privateConstructorBinding() throws Exception {
    Injector injector = Guice.createInjector(new BaseModule() {
      @Override protected void configure() {
        bind(String.class).toInstance("foo");
        bind(FooPrivate.class).toConstructor(getConstructor(FooPrivate.class));
      }
    });
    Assert.assertEquals("foo", injector.getInstance(FooPrivate.class).value);
  }

  @Test(expected = CreationException.class)
  public void tooManyConstructors() throws Exception {
    Guice.createInjector(new BaseModule() {
      @Override protected void configure() {
        bind(String.class).toInstance("foo");
        bind(FooTooMany.class).toConstructor(getConstructor(FooTooMany.class));
      }
    });
  }

  public static class Foo {
    public final String value;

    public Foo(String value) {
      this.value = value;
    }
  }

  public static class FooPrivate {
    public final String value;

    private FooPrivate(String value) {
      this.value = value;
    }
  }

  public static class FooTooMany {
    public final String value;

    private FooTooMany() {
      this("default");
    }

    private FooTooMany(String value) {
      this.value = value;
    }
  }
}
