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
package com.netflix.iep.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.inject.Provider;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

@RunWith(JUnit4.class)
public class DefaultClassFactoryTest {

  @Test
  public void noArgConstructor() {
    ClassFactory factory = new DefaultClassFactory();
    HasDefaultConstructor obj = factory.newInstance(HasDefaultConstructor.class);
    Assert.assertEquals(1, obj.getValue());
  }

  @Test
  public void noArgConstructorWithName() throws Exception {
    ClassFactory factory = new DefaultClassFactory();
    HasDefaultConstructor obj = factory.newInstance(HasDefaultConstructor.class.getName());
    Assert.assertEquals(1, obj.getValue());
  }

  @Test
  public void singleConstructor() {
    Map<Class<?>, Object> bindings = new HashMap<>();
    bindings.put(Integer.TYPE, 1);
    ClassFactory factory = new DefaultClassFactory(bindings::get);
    SingleConstructor obj = factory.newInstance(SingleConstructor.class);
    Assert.assertEquals(1, obj.getValue());
  }

  @Test
  public void singlePrivateConstructor() {
    Map<Class<?>, Object> bindings = new HashMap<>();
    bindings.put(Integer.TYPE, 1);
    ClassFactory factory = new DefaultClassFactory(bindings::get);
    SinglePrivateConstructor obj = factory.newInstance(SinglePrivateConstructor.class);
    Assert.assertEquals(1, obj.getValue());
  }

  @Test
  public void providerConstructor() {
    Map<Type, Object> bindings = new HashMap<>();
    bindings.put(ProviderConstructor.getProviderType(), (Provider<Integer>) () -> 1);
    ClassFactory factory = new DefaultClassFactory(bindings::get);
    ProviderConstructor obj = factory.newInstance(ProviderConstructor.class);
    Assert.assertEquals(1, obj.getValue());
  }

  public static class HasDefaultConstructor {
    private int value = 0;

    public HasDefaultConstructor() {
      value = 1;
    }

    // Make sure other constructors are ignored
    public HasDefaultConstructor(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  public static class SingleConstructor {
    private int value = 0;

    public SingleConstructor(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  public static class SinglePrivateConstructor {
    private int value = 0;

    private SinglePrivateConstructor(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  public static class ProviderConstructor {
    static Type getProviderType() {
      return ProviderConstructor.class.getDeclaredConstructors()[0].getGenericParameterTypes()[0];
    }

    private Provider<Integer> value = null;

    private ProviderConstructor(Provider<Integer> value) {
      this.value = value;
    }

    public int getValue() {
      return value.get();
    }
  }
}
