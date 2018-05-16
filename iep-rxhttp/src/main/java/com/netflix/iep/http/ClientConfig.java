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

import com.netflix.archaius.api.Config;
import io.netty.handler.logging.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Configuration settings to use for making the request. */
class ClientConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientConfig.class);

  private static final Pattern NIWS_URI = Pattern.compile("niws://([^/]+).*");

  private static final Pattern VIP_URI = Pattern.compile("vip://([^:]+):([^/]+).*");

  /** Create relative uri string with the path and query. */
  static String relative(URI uri) {
    String r = uri.getRawPath();
    if (r == null) {
      r = "/";
    } else if (r.startsWith("//")) {
      r = r.substring(1);
    }
    if (uri.getRawQuery() != null) {
      r += "?" + uri.getRawQuery();
    }
    return r;
  }

  private static String fixPath(String path) {
    return (path.startsWith("/http://") || path.startsWith("/https://"))
        ? path.substring(1)
        : path;
  }

  /** Create a client config instance based on a URI. */
  static ClientConfig fromUri(Config config, URI uri, SSLContext context) {
    Matcher m;
    ClientConfig cfg;
    switch (uri.getScheme()) {
      case "niws":
        m = NIWS_URI.matcher(uri.toString());
        if (m.matches()) {
          final URI newUri = URI.create(fixPath(relative(uri)));
          cfg = new ClientConfig(config, m.group(1), null, uri, newUri, context);
        } else {
          throw new IllegalArgumentException("invalid niws uri: " + uri);
        }
        break;
      case "vip":
        m = VIP_URI.matcher(uri.toString());
        if (m.matches()) {
          cfg = new ClientConfig(config, m.group(1), m.group(2), uri, URI.create(relative(uri)), context);
        } else {
          throw new IllegalArgumentException("invalid vip uri: " + uri);
        }
        break;
      default:
        cfg = new ClientConfig(config, "default", null, uri, uri, context);
        break;
    }
    LOGGER.trace(cfg.toString());
    return cfg;
  }

  private final Config config;
  private final String name;
  private final String vipAddress;
  private final URI originalUri;
  private final URI uri;
  private final SSLContext context;

  /** Create a new instance. */
  ClientConfig(
      Config config,
      String name,
      String vipAddress,
      URI originalUri,
      URI uri,
      SSLContext context) {
    this.config = config;
    this.name = name;
    this.vipAddress = vipAddress;
    this.originalUri = originalUri;
    this.uri = uri;
    this.context = context;
  }

  private String dfltProp(String k) {
    return "niws.client." + k;
  }

  private String prop(String k) {
    return name + "." + dfltProp(k);
  }

  private String getString(String k, String dflt) {
    String v = config.getString(prop(k), null);
    return (v == null) ? config.getString(dfltProp(k), dflt) : v;
  }

  private int getInt(String k, int dflt) {
    String v = getString(k, null);
    return (v == null) ? dflt : Integer.parseInt(v);
  }

  private boolean getBoolean(String k, boolean dflt) {
    String v = getString(k, null);
    return (v == null) ? dflt : Boolean.parseBoolean(v);
  }

  /** Name of the client. */
  String name() {
    return name;
  }

  /** Original URI specified before selecting a specific server. */
  URI originalUri() {
    return originalUri;
  }

  /** URI for the request. */
  URI uri() {
    return uri;
  }

  /**
   * Relative URI that should be used for the RxNetty HTTP request. Otherwise RxNetty will include
   * the absolute URI on the first line of the HTTP request:
   *
   * <pre>GET http://localhost:53535/test HTTP/1.1</pre>
   */
  String relativeUri() {
    return relative(uri);
  }

  /** Port to use for the connection. */
  int port(int dflt) {
    return getInt("Port", dflt);
  }

  /** Maximum time to wait for a connection attempt in milliseconds. */
  int connectTimeout() {
    return getInt("ConnectTimeout", 1000);
  }

  /** Maximum time to allow connection to be put into connection pool. */
  int connectionActiveLifeAge() {
    return getInt("ConnectionActiveLifeAge", 0);
  }

  /** RxNetty auto-release content ByteBufs. */
  boolean contentAutoRelease() {
    return getBoolean("ContentAutoRelease", true);
  }

  /** RxNetty auto-dispose content if not subscribed with timeout milliseconds. */
  int contentSubscribeTimeout() {
    return getInt("ContentSubscribeTimeout", 0);
  }

  /** Maximum time to wait for reading data in milliseconds. */
  int readTimeout() {
    return getInt("ReadTimeout", 30000);
  }

  /** Maximum number of redirects to follow. Set to 0 to disable. */
  int followRedirects() {
    return getInt("FollowRedirects", 3);
  }

  /** Maximum number of connections permitted to a single host. */
  int maxConnectionsPerHost() {
    return getInt("MaxConnectionsPerHost", 20);
  }

  /** Maximum number of connections for all clients with the same name. */
  int maxConnectionsTotal() {
    return getInt("MaxConnectionsTotal", 200);
  }

  /** How long in milliseconds a connection can be idle in the pool before being closed. */
  int idleConnectionsTimeoutMillis() {
    return getInt("ConnectionPoolIdleEvictTimeMilliseconds", 60000);
  }

  /** Should HTTPS be used for the request? */
  boolean isSecure() {
    final boolean https = "https".equals(uri.getScheme());
    return https || getBoolean("IsSecure", false);
  }

  /** SSL context that should be used for the request. */
  SSLContext sslContext() {
    return context;
  }

  /**
   * When getting a server list from eureka should the host name or ip address be used? The
   * default is to use the ip address and avoid the dns lookup.
   */
  boolean useIpAddress() {
    return getBoolean("UseIpAddress", true);
  }

  /**
   * Should it attempt to compress the request body and automatically decompress the response
   * body?
   */
  boolean gzipEnabled() {
    return getBoolean("GzipEnabled", true);
  }

  /**
   * Should detailed wire logging be enabled on the underlying client?
   */
  boolean wireLoggingEnabled() {
    return getBoolean("WireLoggingEnabled", false);
  }

  /**
   * Log level to use for logging events.
   */
  LogLevel wireLoggingLevel() {
    return LogLevel.valueOf(getString("WireLoggingLevel", "ERROR"));
  }

  /** Max number of retries. */
  int numRetries() {
    return getInt("MaxAutoRetriesNextServer", 2);
  }

  /**
   * Initial delay to use between retries if a throttled response (429 or 503) is received. The
   * delay will be doubled between each throttled attempt.
   */
  int retryDelay() {
    return getInt("RetryDelay", 500);
  }

  /** Whether to retry read timeouts.  */
  boolean retryReadTimeouts() {
    return getBoolean("RetryReadTimeouts", true);
  }

  /** User agent string to use when making the request. */
  String userAgent() {
    return getString("UserAgent", "RxHttp");
  }

  /** VIP used to lookup a set of servers in eureka. */
  String vip() {
    return (vipAddress == null)
        ? getString("DeploymentContextBasedVipAddresses", null)
        : vipAddress;
  }

  @Override public String toString() {
    return "ClientConfig" +
        "(Name=" + name +
        ",URI=" + uri +
        ",Port=" + port(-1) +
        ",ConnectTimeout=" + connectTimeout() +
        ",ReadTimeout=" + readTimeout() +
        ",ConnectionActiveLifeAge=" + connectionActiveLifeAge() +
        ",ContentAutoRelease=" + contentAutoRelease() +
        ",ContentSubscribeTimeout=" + contentSubscribeTimeout() +
        ",FollowRedirects=" + followRedirects() +
        ",MaxConnectionsPerHost=" + maxConnectionsPerHost() +
        ",MaxConnectionsTotal=" + maxConnectionsTotal() +
        ",ConnectionPoolIdleEvictTimeMilliseconds=" + idleConnectionsTimeoutMillis() +
        ",UseIpAddress=" + useIpAddress() +
        ",GzipEnabled=" + gzipEnabled() +
        ",WireLoggingEnabled=" + wireLoggingEnabled() +
        ",WireLoggingLevel=" + wireLoggingLevel() +
        ",MaxAutoRetriesNextServer=" + numRetries() +
        ",RetryDelay=" + retryDelay() +
        ",RetryReadTimeouts=" + retryReadTimeouts() +
        ",UserAgent=" + userAgent() +
        ",DeploymentContextBasedVipAddresses=" + vip() +
        ")";
  }
}
