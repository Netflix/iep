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
package com.netflix.iep.admin.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigInteger;


@RunWith(JUnit4.class)
public class GuiceDebugEndpointTest {

  final Injector injector = Guice.createInjector(new AbstractModule() {
    @Override protected void configure() {
      bind(String.class).toInstance("foo");
      bind(BigInteger.class).toInstance(BigInteger.TEN);
    }
  });
  private final GuiceDebugEndpoint endpoint = new GuiceDebugEndpoint(injector);

  @Test
  public void get() {
    Assert.assertTrue(endpoint.get().toString().contains("com.google.inject.Injector"));
    Assert.assertTrue(endpoint.get().toString().contains("java.lang.String"));
    Assert.assertTrue(endpoint.get().toString().contains("java.math.BigInteger"));
    Assert.assertFalse(endpoint.get().toString().contains("java.math.BigDecimal"));
  }

  @Test
  public void getString() {
    String response = endpoint.get("java.lang.String/value").toString();
    Assert.assertTrue(response, response.startsWith("ArrayInfo("));
  }

}
