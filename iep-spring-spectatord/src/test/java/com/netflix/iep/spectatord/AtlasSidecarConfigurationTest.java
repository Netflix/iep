/*
 * Copyright 2014-2024 Netflix, Inc.
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
package com.netflix.iep.spectatord;

import com.netflix.spectator.api.Registry;
import com.netflix.spectator.sidecar.SidecarRegistry;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@RunWith(JUnit4.class)
public class AtlasSidecarConfigurationTest {

  @Test
  public void registryIsBound() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(AtlasSidecarConfiguration.class);
      context.refresh();
      context.start();
      Registry registry = context.getBean(Registry.class);
      Assert.assertTrue(registry instanceof SidecarRegistry);
    }
  }
}
