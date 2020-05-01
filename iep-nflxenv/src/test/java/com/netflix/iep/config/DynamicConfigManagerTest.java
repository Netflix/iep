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
package com.netflix.iep.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.atomic.AtomicInteger;

@RunWith(JUnit4.class)
public class DynamicConfigManagerTest {

  private Config config(String... props) {
    String str = String.join("\n", props);
    return ConfigFactory.parseString(str);
  }

  private DynamicConfigManager newInstance(Config baseConfig) {
    return DynamicConfigManager.create(baseConfig);
  }

  @Test
  public void baseConfig() {
    DynamicConfigManager mgr = newInstance(config("a = 1"));
    Assert.assertEquals("1", mgr.get().getString("a"));
  }

  @Test
  public void overrideConfig() {
    DynamicConfigManager mgr = newInstance(config("a = 1", "b = 2"));
    mgr.setOverrideConfig(config("a = 2", "c = 3"));
    Assert.assertEquals("2", mgr.get().getString("a"));
    Assert.assertEquals("2", mgr.get().getString("b"));
    Assert.assertEquals("3", mgr.get().getString("c"));
  }

  @Test
  public void overrideReferencesBase() {
    DynamicConfigManager mgr = newInstance(config("a = 1"));
    mgr.setOverrideConfig(config("b = \"test_\"${a}"));
    Assert.assertEquals("test_1", mgr.get().getString("b"));
  }

  @Test(expected = ConfigException.UnresolvedSubstitution.class)
  public void overrideDoesntResolve() {
    DynamicConfigManager mgr = newInstance(config("a = 1"));
    mgr.setOverrideConfig(config("b = \"test_\"${aa}"));
  }

  @Test
  public void badConfigIsntUsed() {
    DynamicConfigManager mgr = newInstance(config("a = 1"));
    boolean failed = false;
    try {
      mgr.setOverrideConfig(config("b = \"test_\"${aa}", "a = 2"));
    } catch (Exception e) {
      failed = true;
    }
    Assert.assertTrue(failed);
    Assert.assertEquals("1", mgr.get().getString("a"));
  }

  @Test
  public void listener() {
    AtomicInteger value = new AtomicInteger();
    DynamicConfigManager mgr = newInstance(config("a.b = 1"));
    mgr.addListener(ConfigListener.forPath("a", c -> value.set(c.getInt("b"))));
    mgr.setOverrideConfig(config("a.b = 2"));
    Assert.assertEquals(2, value.get());
  }

  @Test
  public void listenerOnlyCalledOnChange() {
    AtomicInteger value = new AtomicInteger();
    DynamicConfigManager mgr = newInstance(config("a.b = 1"));
    mgr.addListener(ConfigListener.forPath("a", c -> value.set(c.getInt("b"))));
    mgr.setOverrideConfig(config("a.b = 1"));
    Assert.assertEquals(0, value.get());
  }

  @Test
  public void listenerFailureIgnored() {
    AtomicInteger value = new AtomicInteger();
    DynamicConfigManager mgr = newInstance(config("a.b = 1"));
    mgr.addListener(ConfigListener.forPath("c", c -> value.addAndGet(c.getInt("b"))));
    mgr.addListener(ConfigListener.forPath("a", c -> value.addAndGet(c.getInt("b"))));
    mgr.setOverrideConfig(config("a.b = 2"));
    Assert.assertEquals(2, value.get());
  }

  @Test
  public void listenerRemove() {
    AtomicInteger value = new AtomicInteger();
    DynamicConfigManager mgr = newInstance(config("a.b = 1"));

    ConfigListener listener = ConfigListener.forPath("a", c -> value.set(c.getInt("b")));
    mgr.addListener(listener);
    mgr.setOverrideConfig(config("a.b = 2"));
    Assert.assertEquals(2, value.get());

    mgr.removeListener(listener);
    mgr.setOverrideConfig(config("a.b = 3"));
    Assert.assertEquals(2, value.get());
  }
}
