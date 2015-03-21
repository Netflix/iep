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
package com.netflix.iep.jmxport;

import com.google.inject.AbstractModule;

public class JmxPortModule extends AbstractModule {

  private static final String JMX_HOST = "netflix.iep.gov.jmxHost";
  private static final String JMX_PORT = "netflix.iep.gov.jmxPort";

  @Override protected void configure() {
    final String host = System.getProperty(JMX_HOST, null);
    final int port = Integer.parseInt(System.getProperty(JMX_PORT, "7500"));
    JmxPort.configure(host, port);
  }
}
