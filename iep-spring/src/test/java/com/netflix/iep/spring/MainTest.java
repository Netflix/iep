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
package com.netflix.iep.spring;

import com.netflix.iep.service.ServiceManager;
import com.netflix.iep.spring.config.TestConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


@RunWith(JUnit4.class)
public class MainTest {

  @BeforeClass
  public static void beforeClass() {
    System.setProperty("netflix.iep.spring.scanPackages", "");
    System.setProperty("netflix.iep.spring.exitOnFailure", "false");
  }

  @Before
  public void before() {
    System.setProperty("netflix.iep.spring.scanPackages", "");
  }

  private AnnotationConfigApplicationContext createContext() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(IepConfiguration.class);
    return context;
  }

  @Test
  public void runWithNoModules() throws Exception {
    Main.run(new String[]{}, createContext());
  }

  @Test
  public void runWithExplicitModules() throws Exception {
    AnnotationConfigApplicationContext context =  createContext();
    context.register(TestConfiguration.class);

    boolean startupFailed = false;
    try {
      Main.run(new String[]{}, context);
    } catch (BeanCreationException e) {
      startupFailed = true;
      Assert.assertEquals("missing required argument", e.getRootCause().getMessage());
    } finally {
      Assert.assertTrue(startupFailed);
    }
  }

  @Test
  public void runWithNoArgs() throws Exception {
    System.setProperty("netflix.iep.spring.scanPackages", "com.netflix.iep.spring.config");
    boolean startupFailed = false;
    try {
      Main.main(new String[]{});
    } catch (BeanCreationException e) {
      startupFailed = true;
      Assert.assertEquals("missing required argument", e.getRootCause().getMessage());
    } finally {
      Assert.assertTrue(startupFailed);
    }
  }

  @Test
  public void runWithUp() throws Exception {
    System.setProperty("netflix.iep.spring.scanPackages", "com.netflix.iep.spring.config");
    ServiceManager sm;
    try (Main m = Main.run(new String[]{"up"})) {
      sm = m.getContext().getBean(ServiceManager.class);
      Assert.assertTrue(sm.isHealthy());
    }
    Assert.assertFalse(sm.isHealthy());
  }

  @Test
  public void runWithDown() throws Exception {
    System.setProperty("netflix.iep.spring.scanPackages", "com.netflix.iep.spring.config");
    ServiceManager sm;
    try (Main m = Main.run(new String[]{"down"})) {
      sm = m.getContext().getBean(ServiceManager.class);
      Assert.assertFalse(sm.isHealthy());
    }
  }

  @Test
  public void runWithFoo() throws Exception {
    System.setProperty("netflix.iep.spring.scanPackages", "com.netflix.iep.spring.config");
    boolean startupFailed = false;
    try {
      Main.run(new String[]{"foo"});
    } catch (BeanCreationException e) {
      startupFailed = true;
      Assert.assertEquals("unknown mode: foo", e.getRootCause().getMessage());
    } finally {
      Assert.assertTrue(startupFailed);
    }
  }
}
