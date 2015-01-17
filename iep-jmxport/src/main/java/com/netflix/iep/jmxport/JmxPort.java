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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.Map;

/**
 * Bind the jmx port to allow it to work through a firewall.
 */
public final class JmxPort {

  private static final Logger LOGGER = LoggerFactory.getLogger(JmxPort.class);

  private static final String META_URL = "http://169.254.169.254/latest/meta-data/";

  private static final String PUBLIC_HOSTNAME = "public-hostname";
  private static final String LOCAL_IPV4 = "local-ipv4";

  private static final int DEFAULT_PORT = 7500;

  private static String getKey(String k) {
    final String urlString = META_URL + k;
    try {
      URL url = new URL(urlString);
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
        return reader.readLine();
      }
    } catch (Exception e) {
      LOGGER.debug("failed to get " + urlString, e);
      return null;
    }
  }

  private JmxPort() {
  }

  public static void configure() {
    configure(null, DEFAULT_PORT);
  }

  public static void configure(String hostname, int port) {
    // Try to get the public hostname, if it can't be determined fall back on local ip
    if (hostname == null) {
      hostname = getKey(PUBLIC_HOSTNAME);
    }
    if (hostname == null) {
      hostname = getKey(LOCAL_IPV4);
    }
    System.setProperty("java.rmi.server.hostname", hostname);
    LOGGER.info("set java.rmi.server.hostname to '" + hostname + "'");

    // Setup server on the configured port
    try {
      LocateRegistry.createRegistry(port);
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      Map<String, Object> env = new HashMap<>();
      JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://"
          + hostname + ":" + port
          + "/jndi/rmi://"
          + hostname + ":" + port
          + "/jmxrmi");

      JMXConnectorServer jmxConn = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);
      LOGGER.info("starting JMX connector server on: " + url.getURLPath());
      jmxConn.start();
    } catch (Exception e) {
      throw new IllegalStateException("failed to initialize jmx port", e);
    }
  }
}
