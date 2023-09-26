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
package com.netflix.iep.atlas;

import com.netflix.spectator.api.Registry;
import com.netflix.spectator.atlas.AtlasRegistry;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

@RunWith(JUnit4.class)
public class AtlasConfigurationTest {

  @Test
  public void registryIsBound() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(AtlasConfiguration.class);
      context.refresh();
      context.start();
      Registry registry = context.getBean(Registry.class);
      Assert.assertTrue(registry instanceof AtlasRegistry);
    }
  }

  @Test(expected = NoSuchBeanDefinitionException.class)
  public void registryIsNotBoundIfSbn2IsPresent() {
    Map<String, Object> props = new HashMap<>();
    props.put("management.metrics.export.atlas.enabled", "true");
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.getEnvironment()
          .getPropertySources()
          .addFirst(new MapPropertySource("test", props));
      context.register(AtlasConfiguration.class);
      context.refresh();
      context.start();
      context.getBean(Registry.class);
    }
  }

  @Test(expected = NoSuchBeanDefinitionException.class)
  public void registryIsNotBoundIfSbn3IsPresent() {
    Map<String, Object> props = new HashMap<>();
    props.put("management.atlas.metrics.export.enabled", "true");
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.getEnvironment()
          .getPropertySources()
          .addFirst(new MapPropertySource("test", props));
      context.register(AtlasConfiguration.class);
      context.refresh();
      context.start();
      context.getBean(Registry.class);
    }
  }
}
