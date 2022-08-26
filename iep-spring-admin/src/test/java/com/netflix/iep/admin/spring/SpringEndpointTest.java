/*
 * Copyright 2014-2022 Netflix, Inc.
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
package com.netflix.iep.admin.spring;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.math.BigInteger;

@RunWith(JUnit4.class)
public class SpringEndpointTest {

  private AnnotationConfigApplicationContext createContext() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.registerBean(String.class, () -> "foo");
    context.registerBean(BigInteger.class, () -> BigInteger.TEN);
    context.refresh();
    return context;
  }

  private final SpringEndpoint endpoint = new SpringEndpoint(createContext());

  @Test
  public void get() {
    Assert.assertTrue(endpoint.get().toString().contains("java.lang.String"));
    Assert.assertTrue(endpoint.get().toString().contains("java.math.BigInteger"));
    Assert.assertFalse(endpoint.get().toString().contains("java.math.BigDecimal"));
  }

  @Test
  public void getWithPath() {
    String response = endpoint.get("math").toString();
    Assert.assertFalse(response.contains("com.google.inject.Injector"));
    Assert.assertFalse(response.contains("java.lang.String"));
    Assert.assertTrue(response.contains("java.math.BigInteger"));
    Assert.assertFalse(response.contains("java.math.BigDecimal"));
  }

  @Test
  public void bindingsMap() {
    /*Map<String, Key<?>> bindings = endpoint.getBindingKeys(v -> true);
    Assert.assertEquals(
        BigInteger.TEN,
        injector.getInstance(bindings.get("java.math.BigInteger")));*/
  }
}
