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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;

import javax.net.ServerSocketFactory;

public class SingleInterfaceRMIServerSocketFactory implements RMIServerSocketFactory {
  private final InetAddress address;

  public SingleInterfaceRMIServerSocketFactory(InetAddress address) {
    this.address = address;
  }

  @Override public ServerSocket createServerSocket(int port) throws IOException {
    return ServerSocketFactory.getDefault().createServerSocket(port, 0, address);
  }
}
