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
package com.netflix.iep.http;

/**
 * Represents a server to try and connect to.
 */
public final class Server {
  /** Id for servers that are not created as part of a server registry. */
  public static final String UNREGISTERED_HOST_ID = "UNREGISTERED";

  private final String id;
  private final String host;
  private final int port;
  private final boolean secure;

  /** Create a new instance with an id based on the other fields. */
  public Server(String host, int port, boolean secure) {
    this(UNREGISTERED_HOST_ID, host, port, secure);
  }

  /** Create a new instance. */
  public Server(String id, String host, int port, boolean secure) {
    this.id = id;
    this.host = host;
    this.port = port;
    this.secure = secure;
  }

  /** Return the unique id for the server. */
  public String id() {
    return id;
  }

  /** Return the host name for the server. */
  public String host() {
    return host;
  }

  /** Return the port for the server. */
  public int port() {
    return port;
  }

  /** Return true if HTTPS should be used. */
  public boolean isSecure() {
    return secure;
  }

  /** Return true if this server is registered with a server registry. */
  public boolean isRegistered() {
    return !UNREGISTERED_HOST_ID.equals(id);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || !(obj instanceof Server)) return false;
    Server other = (Server) obj;
    return id.equals(other.id)
        && host.equals(other.host)
        && port == other.port
        && secure == other.secure;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int hc = prime;
    hc = prime * hc + id.hashCode();
    hc = prime * hc + host.hashCode();
    hc = prime * hc + Integer.valueOf(port).hashCode();
    hc = prime * hc + Boolean.valueOf(secure).hashCode();
    return hc;
  }

  @Override
  public String toString() {
    return "Server(" + id + "," + host + "," + port + "," + secure + ")";
  }
}
