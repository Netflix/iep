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
package com.netflix.iep.config;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.HashMap;

import static org.junit.Assert.*;

import com.netflix.archaius.DefaultPropertyFactory;
import com.netflix.archaius.api.PropertyFactory;
import com.netflix.archaius.config.MapConfig;
import org.junit.Test;

import org.joda.time.DateTime;
import org.joda.time.Duration;

public class ConfigurationTests {
  interface TestConfig extends IConfiguration {
    @DefaultValue("string")
    String getString();

    @DefaultValue("true")
    boolean getBooleanPrimitive();

    @DefaultValue("true")
    Boolean getBooleanObject();

    @DefaultValue("1")
    int getIntegerPrimitive();

    @DefaultValue("1")
    Integer getIntegerObject();

    @DefaultValue("1.1")
    double getDoublePrimitive();

    @DefaultValue("1.1")
    Double getDoubleObject();

    @DefaultValue("2014-08-01T00:00:00")
    DateTime getDateTime();

    @DefaultValue("PT5M")
    Duration getDuration();

    @DefaultValue("2014-08-01T00:00:00")
    ZonedDateTime getJavaDateTime();

    @DefaultValue("UTC")
    ZoneId getZoneUTC();

    @DefaultValue("US/Pacific")
    ZoneId getZonePacific();

    @DefaultValue("PT5M")
    java.time.Duration getJavaDuration();
  }

  private TestConfig mkConfig() { return mkConfig(new HashMap<>()); }
  private TestConfig mkConfig(Map<String,String> props) { return mkConfig(null, props); }
  private TestConfig mkConfig(String prefix, Map<String,String> props) {
    PropertyFactory factory = new DefaultPropertyFactory(new MapConfig(props));
    Configuration.setConfiguration(new DynamicPropertiesConfiguration(factory).getInstance());
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

  @Test
  public void defaultJavaDateTime() {
    TestConfig config = mkConfig();
    ZonedDateTime v = config.getJavaDateTime();
    assertEquals("getDateTime", v.toEpochSecond(), 1406851200L);
  }

  @Test
  public void defaultJavaDuration() {
    TestConfig config = mkConfig();
    java.time.Duration v = config.getJavaDuration();
    assertEquals("getDuration", v.toMillis(), 300000L);
  }

  @Test
  public void zoneUTC() {
    TestConfig config = mkConfig();
    ZoneId v = config.getZoneUTC();
    assertEquals("zoneUTC", v, ZoneId.of("UTC"));
  }

  @Test
  public void zonePacific() {
    TestConfig config = mkConfig();
    ZoneId v = config.getZonePacific();
    assertEquals("zonePacific", v, ZoneId.of("US/Pacific"));
  }
}
