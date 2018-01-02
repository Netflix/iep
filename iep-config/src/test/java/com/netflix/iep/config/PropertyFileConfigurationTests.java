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

import java.util.Map;
import java.util.HashMap;

import static org.junit.Assert.*;

import com.netflix.archaius.DefaultPropertyFactory;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.PropertyFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import org.joda.time.DateTime;
import org.joda.time.Duration;

public class PropertyFileConfigurationTests {

  interface TestConfig extends IConfiguration {
    @DefaultValue("string")
    String getString();

    @DefaultValue("true")
    boolean getBoolean();

    @DefaultValue("1")
    int getInteger();

    @DefaultValue("1.1")
    double getDouble();

    @DefaultValue("2014-08-01T00:00:00")
    DateTime getDateTime();

    @DefaultValue("PT5M")
    Duration getDuration();
  }

  @BeforeClass
  public static void setUp() throws Exception {
    final String userDir = System.getProperty("user.dir");
    Map<String,String> subs = new HashMap<String,String>() {{
      put("user.dir", userDir);
      put("resources.url", "file://" + userDir + "/src/test/resources");
    }};
    Config config = TestResourceConfiguration.load("config.test.properties", subs);
    PropertyFactory factory = new DefaultPropertyFactory(config);
    Configuration.setConfiguration(new DynamicPropertiesConfiguration(factory).getInstance());
  }

  private TestConfig mkConfig(String prefix) {
    return Configuration.newProxy(TestConfig.class, prefix);
  }

  @Test
  public void getString() {
    TestConfig config = mkConfig("netflix");
    String s = config.getString();
    assertEquals("getString", s, "test");
  }

  @Test
  public void getBoolean() {
    TestConfig config = mkConfig("netflix");
    Boolean b = config.getBoolean();
    assertEquals("getBoolean", b, false);
  }

  @Test
  public void getInteger() {
    TestConfig config = mkConfig("netflix");
    Integer i = config.getInteger();
    assertEquals("getInteger", i, Integer.valueOf("2"));
  }

  @Test
  public void getDouble() {
    TestConfig config = mkConfig("netflix");
    Double d = config.getDouble();
    assertEquals("getDouble", d, Double.valueOf("2.1"));
  }

  @Test
  public void getDateTime() {
    TestConfig config = mkConfig("netflix");
    DateTime dt = config.getDateTime();
    assertEquals("getDateTime", dt.getMillis(), 1409529600000L);
  }

  @Test
  public void getDuration() {
    TestConfig config = mkConfig("netflix");
    Duration d = config.getDuration();
    assertEquals("getDuration", d.getMillis(), 600000L);
  }
}
