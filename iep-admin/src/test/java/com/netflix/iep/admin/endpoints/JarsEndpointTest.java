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
package com.netflix.iep.admin.endpoints;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.List;
import java.util.Optional;


@RunWith(JUnit4.class)
public class JarsEndpointTest {

  private final JarsEndpoint endpoint = new JarsEndpoint();

  @Test
  public void get() {
    List<JarsEndpoint.JarInfo> jars = endpoint.jars();
    Assert.assertEquals(jars, endpoint.get());

    // We know spectator-api should be in the classpath and has an Implementation-Version
    // attribute in the manifest
    Optional<JarsEndpoint.JarInfo> spectator = jars.stream()
        .filter(j -> j.getName().contains("spectator-api-"))
        .findFirst();
    Assert.assertTrue(spectator.isPresent());

    JarsEndpoint.JarInfo jar = spectator.get();
    String name = "spectator-api-" + jar.getImplementationVersion() + ".jar";
    Assert.assertEquals(name, new File(jar.getName()).getName());
  }

  @Test
  public void getWithPath() {
    // Should always return null, path isn't supported
    Assert.assertNull(endpoint.get("rt.jar"));
  }
}
