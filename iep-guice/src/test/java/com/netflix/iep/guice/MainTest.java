/*
 * Copyright 2014-2020 Netflix, Inc.
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
import com.google.inject.ProvisionException;
import com.netflix.iep.service.ServiceManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public class MainTest {

  @BeforeClass
  public static void beforeClass() {
    System.setProperty("netflix.iep.guice.exitOnFailure", "false");
  }

  @Before
  public void before() {
    System.setProperty("netflix.iep.guice.useServiceLoader", "true");
    System.setProperty("netflix.iep.guice.modules", "");
  }

  @Test
  public void runWithNoModules() throws Exception {
    System.setProperty("netflix.iep.guice.useServiceLoader", "false");
    Main m = new Main();
    m.runImpl(new String[]{});
  }

  @Test
  public void runWithExplicitModules() throws Exception {
    System.setProperty("netflix.iep.guice.useServiceLoader", "false");
    System.setProperty("netflix.iep.guice.modules", "com.netflix.iep.guice.TestModule");
    Main m = new Main();
    boolean startupFailed = false;
    try {
      m.runImpl(new String[]{});
    } catch (ProvisionException e) {
      startupFailed = true;
      Assert.assertEquals("missing required argument", e.getCause().getMessage());
    } finally {
      Assert.assertTrue(startupFailed);
    }
  }

  @Test
  public void runWithNoArgs() throws Exception {
    Main m = new Main();
    boolean startupFailed = false;
    try {
      m.runImpl(new String[]{});
    } catch (ProvisionException e) {
      startupFailed = true;
      Assert.assertEquals("missing required argument", e.getCause().getMessage());
    } finally {
      Assert.assertTrue(startupFailed);
    }
  }

  @Test
  public void runWithUp() throws Exception {
    Main m = new Main();
    m.runImpl(new String[]{"up"});
    Assert.assertTrue(m.getHelper().getInjector().getInstance(ServiceManager.class).isHealthy());
  }

  @Test
  public void runWithDown() throws Exception {
    Main m = new Main();
    m.runImpl(new String[]{"down"});
    Assert.assertFalse(m.getHelper().getInjector().getInstance(ServiceManager.class).isHealthy());
  }

  @Test
  public void runWithFoo() throws Exception {
    Main m = new Main();
    boolean startupFailed = false;
    try {
      m.runImpl(new String[]{"foo"});
    } catch (ProvisionException e) {
      startupFailed = true;
      Assert.assertEquals("unknown mode: foo", e.getCause().getMessage());
    } finally {
      Assert.assertTrue(startupFailed);
    }
  }

  @Test
  public void runWithAdditionalModules() throws Exception {
    Module module = new AbstractModule() {
      @Override protected void configure() {
        bind(String.class).toInstance("foo");
      }
    };
    Main m = new Main();
    m.runImpl(new String[]{"up"}, module);
    Assert.assertEquals("foo", m.getHelper().getInjector().getInstance(String.class));
  }
}
