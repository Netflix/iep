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
package com.netflix.iep.jmxport;

import com.google.inject.AbstractModule;

public final class JmxPortModule extends AbstractModule {

  private static final String JMX_HOST = "netflix.iep.jmx.host";
  private static final String JMX_PORT = "netflix.iep.jmx.port";

  @Override protected void configure() {
    final String host = System.getProperty(JMX_HOST, "localhost");
    final int port = Integer.parseInt(System.getProperty(JMX_PORT, "7500"));
    JmxPort.configure(host, port);
  }

  @Override public boolean equals(Object obj) {
    return obj != null && getClass().equals(obj.getClass());
  }

  @Override public int hashCode() {
    return getClass().hashCode();
  }
}
