/*
 * Copyright 2014-2023 Netflix, Inc.
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
import com.typesafe.config.ConfigMemorySize;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
    Assert.assertEquals(1, value.get());
    mgr.setOverrideConfig(config("a.b = 2"));
    Assert.assertEquals(2, value.get());
  }

  @Test
  public void listenerOnlyCalledOnChange() {
    AtomicInteger value = new AtomicInteger();
    DynamicConfigManager mgr = newInstance(config("a.b = 1"));
    mgr.addListener(ConfigListener.forPath("a", c -> {
      int v = c.getInt("b");
      if (v == value.get()) {
        Assert.fail("listener invoked without a change in the value");
      }
      value.set(v);
    }));
    mgr.setOverrideConfig(config("a.b = 1"));
  }

  @Test
  public void listenerFailureIgnored() {
    AtomicInteger value = new AtomicInteger();
    DynamicConfigManager mgr = newInstance(config("a.b = 1"));
    mgr.addListener(ConfigListener.forPath("c", c -> value.addAndGet(c.getInt("b"))));
    mgr.addListener(ConfigListener.forPath("a", c -> value.addAndGet(c.getInt("b"))));
    mgr.setOverrideConfig(config("a.b = 2"));
    Assert.assertEquals(3, value.get());
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

  @Test
  public void configListener() {
    AtomicReference<Config> value = new AtomicReference<>();
    DynamicConfigManager mgr = newInstance(config("a.b = 1"));
    mgr.addListener(ConfigListener.forConfig("a", value::set));

    mgr.setOverrideConfig(config("a.b = 2"));
    Assert.assertEquals(2, value.get().getInt("b"));

    mgr.setOverrideConfig(config("a.b = null"));
    Assert.assertFalse(value.get().hasPath("b"));

    mgr.setOverrideConfig(config("a = null"));
    Assert.assertNull(value.get());

    mgr.setOverrideConfig(config("a.b = \"foo\""));
    Assert.assertEquals("foo", value.get().getString("b"));
  }

  @Test
  public void configListListener() {
    AtomicReference<List<? extends Config>> value = new AtomicReference<>();
    DynamicConfigManager mgr = newInstance(config("a.b = [{c=1},{c=2}]"));
    mgr.addListener(ConfigListener.forConfigList("a.b", value::set));
    Assert.assertEquals(2, value.get().size());

    mgr.setOverrideConfig(config("a.b = []"));
    Assert.assertTrue(value.get().isEmpty());
  }

  @Test
  public void stringListener() {
    AtomicReference<String> value = new AtomicReference<>();
    DynamicConfigManager mgr = newInstance(config("a.b = 1"));
    mgr.addListener(ConfigListener.forString("a.b", value::set));

    mgr.setOverrideConfig(config("a.b = 2"));
    Assert.assertEquals("2", value.get());

    mgr.setOverrideConfig(config("a.b = null"));
    Assert.assertNull(value.get());

    mgr.setOverrideConfig(config("a.b = \"foo\""));
    Assert.assertEquals("foo", value.get());
  }

  @Test(expected = NullPointerException.class)
  public void stringListenerNullProp() {
    ConfigListener.forString(null, s -> {});
  }

  @Test
  public void stringListListener() {
    AtomicReference<List<String>> value = new AtomicReference<>();
    DynamicConfigManager mgr = newInstance(config("a.b = [1,2]"));
    mgr.addListener(ConfigListener.forStringList("a.b", value::set));

    mgr.setOverrideConfig(config("a.b = [\"foo\"]"));
    Assert.assertEquals(Collections.singletonList("foo"), value.get());
  }

  @Test
  public void booleanListener() {
    AtomicReference<Boolean> value = new AtomicReference<>();
    DynamicConfigManager mgr = newInstance(config("a.b = false"));
    mgr.addListener(ConfigListener.forBoolean("a.b", value::set));
    Assert.assertFalse(value.get());
    mgr.setOverrideConfig(config("a.b = true"));
    Assert.assertTrue(value.get());
  }

  @Test
  public void booleanListListener() {
    AtomicReference<List<Boolean>> value = new AtomicReference<>();
    DynamicConfigManager mgr = newInstance(config("a.b = [false]"));
    mgr.addListener(ConfigListener.forBooleanList("a.b", value::set));
    Assert.assertFalse(value.get().get(0));
    mgr.setOverrideConfig(config("a.b = [true]"));
    Assert.assertTrue(value.get().get(0));
  }

  @Test
  public void intListener() {
    AtomicReference<Integer> value = new AtomicReference<>();
    DynamicConfigManager mgr = newInstance(config("a.b = 1"));
    mgr.addListener(ConfigListener.forInt("a.b", value::set));

    mgr.setOverrideConfig(config("a.b = 2"));
    Assert.assertEquals((Integer) 2, value.get());

    mgr.setOverrideConfig(config("a.b = null"));
    Assert.assertNull(value.get());

    mgr.setOverrideConfig(config("a.b = \"foo\"")); // fails to update, wrong type
    Assert.assertNull(value.get());
  }

  @Test
  public void intListListener() {
    AtomicReference<List<Integer>> value = new AtomicReference<>();
    DynamicConfigManager mgr = newInstance(config("a.b = [1]"));
    mgr.addListener(ConfigListener.forIntList("a.b", value::set));
    Assert.assertEquals((Integer) 1, value.get().get(0));
    mgr.setOverrideConfig(config("a.b = [2]"));
    Assert.assertEquals((Integer) 2, value.get().get(0));
  }

  @Test
  public void longListener() {
    AtomicReference<Long> value = new AtomicReference<>();
    DynamicConfigManager mgr = newInstance(config("a.b = 1"));
    mgr.addListener(ConfigListener.forLong("a.b", value::set));
    Assert.assertEquals((Long) 1L, value.get());
    mgr.setOverrideConfig(config("a.b = 2"));
    Assert.assertEquals((Long) 2L, value.get());
  }

  @Test
  public void longListListener() {
    AtomicReference<List<Long>> value = new AtomicReference<>();
    DynamicConfigManager mgr = newInstance(config("a.b = [1]"));
    mgr.addListener(ConfigListener.forLongList("a.b", value::set));
    Assert.assertEquals((Long) 1L, value.get().get(0));
    mgr.setOverrideConfig(config("a.b = [2]"));
    Assert.assertEquals((Long) 2L, value.get().get(0));
  }

  @Test
  public void bytesListener() {
    AtomicReference<Long> value = new AtomicReference<>();
    DynamicConfigManager mgr = newInstance(config("a.b = 1k"));
    mgr.addListener(ConfigListener.forBytes("a.b", value::set));
    Assert.assertEquals((Long) 1024L, value.get());
    mgr.setOverrideConfig(config("a.b = 2k"));
    Assert.assertEquals((Long) 2048L, value.get());
  }

  @Test
  public void bytesListListener() {
    AtomicReference<List<Long>> value = new AtomicReference<>();
    DynamicConfigManager mgr = newInstance(config("a.b = [1k]"));
    mgr.addListener(ConfigListener.forBytesList("a.b", value::set));
    Assert.assertEquals((Long) 1024L, value.get().get(0));
    mgr.setOverrideConfig(config("a.b = [2k]"));
    Assert.assertEquals((Long) 2048L, value.get().get(0));
  }

  @Test
  public void memorySizeListener() {
    AtomicReference<ConfigMemorySize> value = new AtomicReference<>();
    DynamicConfigManager mgr = newInstance(config("a.b = 1k"));
    mgr.addListener(ConfigListener.forMemorySize("a.b", value::set));
    Assert.assertEquals(ConfigMemorySize.ofBytes(1024L), value.get());
    mgr.setOverrideConfig(config("a.b = 2k"));
    Assert.assertEquals(ConfigMemorySize.ofBytes(2048L), value.get());
  }

  @Test
  public void memorySizeListListener() {
    AtomicReference<List<ConfigMemorySize>> value = new AtomicReference<>();
    DynamicConfigManager mgr = newInstance(config("a.b = [1k]"));
    mgr.addListener(ConfigListener.forMemorySizeList("a.b", value::set));
    Assert.assertEquals(ConfigMemorySize.ofBytes(1024L), value.get().get(0));
    mgr.setOverrideConfig(config("a.b = [2k]"));
    Assert.assertEquals(ConfigMemorySize.ofBytes(2048L), value.get().get(0));
  }

  @Test
  public void doubleListener() {
    AtomicReference<Double> value = new AtomicReference<>();
    DynamicConfigManager mgr = newInstance(config("a.b = 1.0"));
    mgr.addListener(ConfigListener.forDouble("a.b", value::set));
    Assert.assertEquals((Double) 1.0, value.get());
    mgr.setOverrideConfig(config("a.b = 2.0"));
    Assert.assertEquals((Double) 2.0, value.get());
  }

  @Test
  public void doubleListListener() {
    AtomicReference<List<Double>> value = new AtomicReference<>();
    DynamicConfigManager mgr = newInstance(config("a.b = [1.0]"));
    mgr.addListener(ConfigListener.forDoubleList("a.b", value::set));
    Assert.assertEquals((Double) 1.0, value.get().get(0));
    mgr.setOverrideConfig(config("a.b = [2.0]"));
    Assert.assertEquals((Double) 2.0, value.get().get(0));
  }

  @Test
  public void numberListener() {
    AtomicReference<Number> value = new AtomicReference<>();
    DynamicConfigManager mgr = newInstance(config("a.b = 1.0"));
    mgr.addListener(ConfigListener.forNumber("a.b", value::set));
    Assert.assertEquals(1, value.get());
    mgr.setOverrideConfig(config("a.b = 2.0"));
    Assert.assertEquals(2, value.get());
  }

  @Test
  public void numberListListener() {
    AtomicReference<List<Number>> value = new AtomicReference<>();
    DynamicConfigManager mgr = newInstance(config("a.b = [1.0]"));
    mgr.addListener(ConfigListener.forNumberList("a.b", value::set));
    Assert.assertEquals(1, value.get().get(0));
    mgr.setOverrideConfig(config("a.b = [2.0]"));
    Assert.assertEquals(2, value.get().get(0));
  }

  @Test
  public void durationListener() {
    AtomicReference<Duration> value = new AtomicReference<>();
    DynamicConfigManager mgr = newInstance(config("a.b = 1d"));
    mgr.addListener(ConfigListener.forDuration("a.b", value::set));
    Assert.assertEquals(Duration.ofDays(1), value.get());
    mgr.setOverrideConfig(config("a.b = 2d"));
    Assert.assertEquals(Duration.ofDays(2), value.get());
  }

  @Test
  public void durationListListener() {
    AtomicReference<List<Duration>> value = new AtomicReference<>();
    DynamicConfigManager mgr = newInstance(config("a.b = [1d]"));
    mgr.addListener(ConfigListener.forDurationList("a.b", value::set));
    Assert.assertEquals(Duration.ofDays(1), value.get().get(0));
    mgr.setOverrideConfig(config("a.b = [2d]"));
    Assert.assertEquals(Duration.ofDays(2), value.get().get(0));
  }

  @Test
  public void periodListener() {
    AtomicReference<Period> value = new AtomicReference<>();
    DynamicConfigManager mgr = newInstance(config("a.b = 1d"));
    mgr.addListener(ConfigListener.forPeriod("a.b", value::set));
    Assert.assertEquals(Period.ofDays(1), value.get());
    mgr.setOverrideConfig(config("a.b = 2d"));
    Assert.assertEquals(Period.ofDays(2), value.get());
  }

  @Test
  public void temporalListener() {
    AtomicReference<TemporalAmount> value = new AtomicReference<>();
    DynamicConfigManager mgr = newInstance(config("a.b = 1d"));
    mgr.addListener(ConfigListener.forTemporal("a.b", value::set));
    Assert.assertEquals(Duration.ofDays(1), value.get());
    mgr.setOverrideConfig(config("a.b = 2d"));
    Assert.assertEquals(Duration.ofDays(2), value.get());
  }
}
