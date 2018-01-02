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
import com.google.inject.CreationException;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class OverrideTest {

  @Test
  public void dedup() throws Exception {
    GuiceHelper helper = new GuiceHelper();
    helper.start(new ModuleA(), new ModuleA());
    Assert.assertEquals("A", helper.getInjector().getInstance(String.class));
    helper.shutdown();
  }

  @Test(expected = CreationException.class)
  public void conflict() throws Exception {
    GuiceHelper helper = new GuiceHelper();
    helper.start(new ModuleA(), new ModuleB());
    Assert.assertEquals("A", helper.getInjector().getInstance(String.class));
    helper.shutdown();
  }

  @Test
  public void override() throws Exception {
    GuiceHelper helper = new GuiceHelper();
    helper.start(new ModuleC());
    Assert.assertEquals("B", helper.getInjector().getInstance(String.class));
    helper.shutdown();
  }

  @Test(expected = CreationException.class)
  public void dedupWithOverride() throws Exception {
    GuiceHelper helper = new GuiceHelper();
    helper.start(new ModuleA(), new ModuleC());
    Assert.assertEquals("B", helper.getInjector().getInstance(String.class));
    helper.shutdown();
  }

  private static class ModuleA extends AbstractModule {
    @Override protected void configure() {
      bind(String.class).toInstance("A");
    }
  }

  private static class ModuleB extends AbstractModule {
    @Override protected void configure() {
      bind(String.class).toInstance("B");
    }
  }

  private static class ModuleC extends AbstractModule {
    @Override protected void configure() {
      Module m = Modules.override(new ModuleA()).with(new ModuleB());
      install(m);
    }
  }
}
