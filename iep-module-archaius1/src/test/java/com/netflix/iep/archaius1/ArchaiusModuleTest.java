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
package com.netflix.iep.archaius1;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.api.inject.RemoteLayer;
import com.netflix.archaius.api.inject.RuntimeLayer;
import com.netflix.archaius.bridge.StaticAbstractConfiguration;
import com.netflix.archaius.bridge.StaticDeploymentContext;
import com.netflix.archaius.config.DefaultSettableConfig;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.guice.ArchaiusModule;
import org.apache.commons.configuration.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public class ArchaiusModuleTest {

  private static final Key<Configuration> IEP_CONFIG = Key.get(Configuration.class, Names.named("IEP"));

  private final Module overrideModule = new ArchaiusModule() {
    @Override protected void configureArchaius() {
      try {
        MapConfig cfg = MapConfig.builder()
            .put("netflix.iep.archaius.scope.environment", "test")
            .put("netflix.iep.archaius.scope.datacenter",  "cloud")
            .put("netflix.iep.archaius.scope.appId",       "local")
            .put("netflix.iep.archaius.scope.cluster",     "local-dev")
            .put("netflix.iep.archaius.scope.asg",         "local-dev-v000")
            .put("netflix.iep.archaius.scope.serverId",    "localhost")
            .put("netflix.iep.archaius.scope.stack",       "dev")
            .put("netflix.iep.archaius.scope.region",      "us-east-1")
            .put("netflix.iep.archaius.scope.zone",        "us-east-1a")
            .put("a", "b")
            .put("c", "d")
            .build();
        bindApplicationConfigurationOverride().toInstance(cfg);

        DefaultSettableConfig dynamic = new DefaultSettableConfig();
        dynamic.setProperty("c", "dynamic");

        bind(DefaultSettableConfig.class).annotatedWith(RemoteLayer.class).toInstance(dynamic);
        bind(Config.class).annotatedWith(RemoteLayer.class).toInstance(dynamic);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  };

  private final Module testModule = Modules
      .override(new ArchaiusModule(), new Archaius1Module())
      .with(overrideModule);

  @Before
  public void init() {
    StaticAbstractConfiguration.reset();
    StaticDeploymentContext.reset();
  }

  @Test
  @Ignore
  public void getValues() {
    Configuration cfg = Guice.createInjector(testModule).getInstance(IEP_CONFIG);
    Assert.assertEquals("b", cfg.getString("a"));
    Assert.assertEquals("dynamic", cfg.getString("c"));
  }

  @Test
  public void getValueRuntime() {
    Key<SettableConfig> key = Key.get(SettableConfig.class, RuntimeLayer.class);
    Injector injector = Guice.createInjector(testModule);
    SettableConfig runtime = injector.getInstance(key);
    Configuration root = injector.getInstance(IEP_CONFIG);

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
