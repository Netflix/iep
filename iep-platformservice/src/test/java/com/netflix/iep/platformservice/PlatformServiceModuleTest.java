/*
 * Copyright 2015 Netflix, Inc.
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
package com.netflix.iep.platformservice;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import com.netflix.archaius.Config;
import com.netflix.archaius.config.DefaultSettableConfig;
import com.netflix.archaius.config.SettableConfig;
import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.archaius.inject.OverrideLayer;
import com.netflix.archaius.inject.RuntimeLayer;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public class PlatformServiceModuleTest {

  private Module overrideModule = new AbstractModule() {
    @Override protected void configure() {
      bind(com.typesafe.config.Config.class).toInstance(ConfigFactory.parseString("a=b\nc=d"));

      DefaultSettableConfig dynamic = new DefaultSettableConfig();
      dynamic.setProperty("c", "dynamic");

      bind(DefaultSettableConfig.class).annotatedWith(OverrideLayer.class).toInstance(dynamic);
      bind(Config.class).annotatedWith(OverrideLayer.class).toInstance(dynamic);
    }
  };

  private Module baseModule = Modules.override(new ArchaiusModule()).with(new PlatformServiceModule());
  private Module testModule = Modules.override(baseModule).with(overrideModule);

  @Test
  public void getValues() {
    Config cfg = Guice.createInjector(testModule).getInstance(Config.class);
    Assert.assertEquals("b", cfg.getString("a"));
    Assert.assertEquals("dynamic", cfg.getString("c"));
  }

  @Test
  public void runtime() {
    Key<SettableConfig> key = Key.get(SettableConfig.class, RuntimeLayer.class);
    Injector injector = Guice.createInjector(testModule);
    SettableConfig c1 = injector.getInstance(key);
    SettableConfig c2 = injector.getInstance(key);
    Assert.assertSame(c1, c2);
  }

  @Test
  public void getValueRuntime() {
    Key<SettableConfig> key = Key.get(SettableConfig.class, RuntimeLayer.class);
    Injector injector = Guice.createInjector(testModule);
    SettableConfig runtime = injector.getInstance(key);
    Config root = injector.getInstance(Config.class);

    Assert.assertEquals("b", root.getString("a"));
    Assert.assertEquals("dynamic", root.getString("c"));

    runtime.setProperty("a", "runtime");
    runtime.setProperty("c", "runtime");
    Assert.assertEquals("runtime", root.getString("a"));
    Assert.assertEquals("runtime", root.getString("c"));

    runtime.clearProperty("a");
    Assert.assertEquals("b", root.getString("a"));
    Assert.assertEquals("runtime", root.getString("c"));
  }
}
