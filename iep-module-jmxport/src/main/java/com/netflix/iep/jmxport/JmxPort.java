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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RMIServerSocketFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * Bind the jmx port to allow it to work through a firewall.
 */
public final class JmxPort {

  private static final Logger LOGGER = LoggerFactory.getLogger(JmxPort.class);

  private static final String DEFAULT_HOST = "localhost";
  private static final int DEFAULT_PORT = 7500;

  private JmxPort() {
  }

  public static void configure() {
    configure(DEFAULT_HOST, DEFAULT_PORT);
  }

  public static void configure(String hostname, int port) {
    if (hostname == null) {
      throw new IllegalArgumentException("hostname cannot be null");
    }
    System.setProperty("java.rmi.server.hostname", hostname);
    LOGGER.info("set java.rmi.server.hostname to '" + hostname + "'");

    // Setup server on the configured port
    try {
      InetAddress address = InetAddress.getByName(hostname);
      RMIServerSocketFactory factory = new SingleInterfaceRMIServerSocketFactory(address);
      LocateRegistry.createRegistry(port, null, factory);
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      Map<String, Object> env = new HashMap<>();
      env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE,  factory);
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

  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      System.err.println("Usage: jmxport <hostname> <port>");
      System.exit(1);
    }

    final String hostname = args[0];
    final int port = Integer.parseInt(args[1]);
    configure(hostname, port);

    System.out.println("Press any key to exit...");
    System.in.read();
  }
}
