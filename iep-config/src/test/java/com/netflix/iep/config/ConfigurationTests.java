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
package com.netflix.iep.config;

import java.util.Map;
import java.util.HashMap;

import static org.junit.Assert.*;

import com.netflix.archaius.AppConfig;
import com.netflix.archaius.DefaultAppConfig;
import org.junit.Test;

import org.joda.time.DateTime;
import org.joda.time.Duration;

public class ConfigurationTests {
  interface TestConfig extends IConfiguration {
    @DefaultValue("string")
    public String getString();

    @DefaultValue("true")
    public boolean getBooleanPrimitive();

    @DefaultValue("true")
    public Boolean getBooleanObject();

    @DefaultValue("1")
    public int getIntegerPrimitive();

    @DefaultValue("1")
    public Integer getIntegerObject();

    @DefaultValue("1.1")
    public double getDoublePrimitive();

    @DefaultValue("1.1")
    public Double getDoubleObject();

    @DefaultValue("2014-08-01T00:00:00")
    public DateTime getDateTime();

    @DefaultValue("PT5M")
    public Duration getDuration();
  }

  private TestConfig mkConfig() { return mkConfig(new HashMap<String,String>()); }
  private TestConfig mkConfig(Map<String,String> props) { return mkConfig(null, props); }
  private TestConfig mkConfig(String prefix, Map<String,String> props) {
    AppConfig config = DefaultAppConfig.createDefault();
    for (Map.Entry<String,String> e : props.entrySet())
      config.setProperty(e.getKey(), e.getValue());
    Configuration.setConfiguration(new DynamicPropertiesConfiguration(config));
    return Configuration.newProxy(TestConfig.class, prefix);
  }

  @Test
  public void withNullPrefix() {
    Map<String,String> props = new HashMap<String,String>() {{
      put("getString", "test");
    }};
    TestConfig config = mkConfig(props);
    String s = config.getString();
    assertEquals("getString", s, "test");
  }

  @Test
  public void withPrefix() {
    Map<String,String> props = new HashMap<String,String>() {{
      put("netflix.config.getString", "test");
    }};
    TestConfig config = mkConfig("netflix.config", props);
    String s = config.getString();
    assertEquals("getString", s, "test");
  }

  @Test
  public void defaultString() {
    TestConfig config = mkConfig();
    String s = config.getString();
    assertEquals("getString", s, "string");
  }

  @Test
  public void defaultBoolean() {
    TestConfig config = mkConfig();
    boolean p = config.getBooleanPrimitive();
    assertTrue("getBooleanPrimitive", p);
    Boolean o = config.getBooleanObject();
    assertEquals("getBooleanObject", o, Boolean.TRUE);
  }

  @Test
  public void overrideBoolean() {
    Map<String,String> props = new HashMap<String,String>() {{
      put("getBooleanPrimitive", "false");
      put("getBooleanObject", "false");
    }};
    TestConfig config = mkConfig(props);
    boolean p = config.getBooleanPrimitive();
    assertTrue("getBooleanPrimitive", !p);
    Boolean o = config.getBooleanObject();
    assertEquals("getBooleanObject", o, Boolean.FALSE);
  }

  @Test
  public void defaultInteger() {
    TestConfig config = mkConfig();
    int p = config.getIntegerPrimitive();
    assertEquals("getIntegerPrimitive", p, 1);
    Integer o = config.getIntegerObject();
    assertEquals("getIntegerObject", o, Integer.valueOf("1"));
  }

  @Test
  public void overrideInteger() {
    Map<String,String> props = new HashMap<String,String>() {{
      put("getIntegerPrimitive", "2");
      put("getIntegerObject", "2");
    }};
    TestConfig config = mkConfig(props);
    int p = config.getIntegerPrimitive();
    assertEquals("getIntegerPrimitive", p, 2);
    Integer o = config.getIntegerObject();
    assertEquals("getIntegerObject", o, Integer.valueOf("2"));
  }

  @Test
  public void defaultDouble() {
    TestConfig config = mkConfig();
    double p = config.getDoublePrimitive();
    assertEquals("getDoublePrimitive", p, 1.1, 0.0000000001);
    Double o = config.getDoubleObject();
    assertEquals("getDoubleObject", o, Double.valueOf("1.1"));
  }

  @Test
  public void overrideDouble() {
    Map<String,String> props = new HashMap<String,String>() {{
      put("getDoublePrimitive", "2.2");
      put("getDoubleObject", "2.2");
    }};
    TestConfig config = mkConfig(props);
    double p = config.getDoublePrimitive();
    assertEquals("getDoublePrimitive", p, 2.2, 0.0000000001);
    Double o = config.getDoubleObject();
    assertEquals("getDoubleObject", o, Double.valueOf("2.2"));
  }

  @Test
  public void defaultDateTime() {
    TestConfig config = mkConfig();
    DateTime v = config.getDateTime();
    assertEquals("getDateTime", v.getMillis(), 1406851200000L);
  }

  @Test
  public void defaultDuration() {
    TestConfig config = mkConfig();
    Duration v = config.getDuration();
    assertEquals("getDuration", v.getMillis(), 300000L);
  }
}
