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

import com.typesafe.config.Config;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ConfigManagerTest {

  @Test
  public void accountConfig() {
    Assert.assertTrue(ConfigManager.get().getBoolean("iep.account-config-loaded"));
  }

  @Test
  public void loadFromFile() {
    Assert.assertTrue(ConfigManager.get().getBoolean("iep.file-include-loaded"));
  }

  @Test
  public void loadFromClasspath() {
    Assert.assertTrue(ConfigManager.get().getBoolean("iep.classpath-include-loaded"));
  }

  @Test
  public void includeOverrides() {
    Assert.assertEquals("classpath", ConfigManager.get().getString("iep.value"));
  }

  @Test
  public void includeDoesNotOverrideSubstitutions() {
    Assert.assertEquals("reference", ConfigManager.get().getString("iep.substitute"));
  }

  @Test
  public void nullContextClassLoader() {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(null);
      Config config = ConfigManager.load();
      Assert.assertEquals("foo", config.getString("netflix.iep.env.account-type"));
    } finally {
      Thread.currentThread().setContextClassLoader(cl);
    }
  }
}
