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
package com.netflix.iep.aws;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Factory for creating instances of AWS clients.
 */
@Singleton
public class AwsClientFactory implements AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(AwsClientFactory.class);

  private final ConcurrentHashMap<String, AmazonWebServiceClient> clients = new ConcurrentHashMap<>();

  private final Config config;
  private final String region;

  @Inject
  public AwsClientFactory(Config config) {
    this.config = config;
    this.region = config.getString("netflix.iep.aws.region");
  }

  private String firstOnly(String path) {
    int pos = path.indexOf(".");
    return (pos != -1) ? path.substring(0, pos) : path;
  }

  String getDefaultName(Class<?> cls) {
    final String prefix = "com.amazonaws.services.";
    String pkg = cls.getPackage().getName();
    return pkg.startsWith(prefix) ? firstOnly(pkg.substring(prefix.length())) : null;
  }

  private <T> void setIfPresent(Config cfg, String key, Function<String, T> getter, Consumer<T> setter) {
    if (cfg.hasPath(key)) {
      setter.accept(getter.apply(key));
    }
  }

  private Config getConfig(String name, String suffix) {
    final String cfgPrefix = "netflix.iep.aws";
    return (name != null && config.hasPath(cfgPrefix + "." + name + "." + suffix))
        ? config.getConfig(cfgPrefix + "." + name + "." + suffix)
        : config.getConfig(cfgPrefix + ".default." + suffix);
  }

  ClientConfiguration createClientConfig(String name) {
    final Config cfg = getConfig(name, "client");
    final ClientConfiguration settings = new ClientConfiguration();

    // Should be the default, but just to make it explicit
    settings.setProtocol(Protocol.HTTPS);

    // Helpers
    Function<String, Long> getMillis = k -> cfg.getDuration(k, TimeUnit.MILLISECONDS);
    Function<String, Integer> getTimeout = k -> getMillis.apply(k).intValue();

    // Typically use the defaults
    setIfPresent(cfg, "use-gzip",             cfg::getBoolean, settings::setUseGzip);
    setIfPresent(cfg, "use-reaper",           cfg::getBoolean, settings::setUseReaper);
    setIfPresent(cfg, "use-tcp-keep-alive",   cfg::getBoolean, settings::setUseTcpKeepAlive);
    setIfPresent(cfg, "use-throttle-retries", cfg::getBoolean, settings::setUseThrottleRetries);
    setIfPresent(cfg, "max-connections",      cfg::getInt,     settings::setMaxConnections);
    setIfPresent(cfg, "max-error-retry",      cfg::getInt,     settings::setMaxErrorRetry);
    setIfPresent(cfg, "connection-ttl",       getMillis,       settings::setConnectionTTL);
    setIfPresent(cfg, "connection-max-idle",  getMillis,       settings::setConnectionMaxIdleMillis);
    setIfPresent(cfg, "connection-timeout",   getTimeout,      settings::setConnectionTimeout);
    setIfPresent(cfg, "socket-timeout",       getTimeout,      settings::setSocketTimeout);
    setIfPresent(cfg, "client-execution-timeout", getTimeout,      settings::setClientExecutionTimeout);
    setIfPresent(cfg, "user-agent-prefix",    cfg::getString,  settings::setUserAgentPrefix);
    setIfPresent(cfg, "user-agent-suffix",    cfg::getString,  settings::setUserAgentSuffix);
    setIfPresent(cfg, "proxy-port",           cfg::getInt,     settings::setProxyPort);
    setIfPresent(cfg, "proxy-host",           cfg::getString,  settings::setProxyHost);
    setIfPresent(cfg, "proxy-domain",         cfg::getString,  settings::setProxyDomain);
    setIfPresent(cfg, "proxy-workstation",    cfg::getString,  settings::setProxyWorkstation);
    setIfPresent(cfg, "proxy-username",       cfg::getString,  settings::setProxyUsername);
    setIfPresent(cfg, "proxy-password",       cfg::getString,  settings::setProxyPassword);
    return settings;
  }

  private String createRoleArn(String arnPattern, String accountId) {
    final boolean needsSubstitution = arnPattern.contains("{account}");
    if (accountId == null) {
      if (needsSubstitution) {
        throw new IllegalArgumentException("missing account id for ARN pattern: " + arnPattern);
      }
      return arnPattern;
    } else if (needsSubstitution) {
      return arnPattern.replace("{account}", accountId);
    } else {
      LOGGER.warn("requested account, {}, is not used by ARN pattern: {}", accountId, arnPattern);
      return arnPattern;
    }
  }

  private AWSCredentialsProvider createAssumeRoleProvider(
      Config cfg, String accountId, AWSCredentialsProvider p) {
    final String arn = createRoleArn(cfg.getString("role-arn"), accountId);
    final String name = cfg.getString("role-session-name");
    final AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClient.builder()
        .withCredentials(p)
        .withRegion(region)
        .build();
    return new STSAssumeRoleSessionCredentialsProvider.Builder(arn, name)
        .withStsClient(stsClient)
        .build();
  }

  AWSCredentialsProvider createCredentialsProvider(String name, String accountId) {
    final AWSCredentialsProvider dflt = new DefaultAWSCredentialsProviderChain();
    final Config cfg = getConfig(name, "credentials");
    if (cfg.hasPath("role-arn")) {
      return createAssumeRoleProvider(cfg, accountId, dflt);
    } else {
      if (accountId == null) {
        LOGGER.warn("requested account, {}, ignored, no role ARN configured", accountId);
      }
      return dflt;
    }
  }

  private Class<?> getClientClass(Class<?> cls) throws Exception {
    if (cls.isInterface()) {
      return Class.forName(cls.getName() + "Client");
    } else {
      return cls;
    }
  }

  private String chooseRegion(String name, Class<?> cls) {
    final String nameProp = "netflix.iep.aws." + name + ".region";
    final String service = getDefaultName(cls);
    final String dfltProp = "netflix.iep.aws.endpoint." + service + "." + region;
    String endpointRegion = region;
    if (config.hasPath(nameProp)) {
      endpointRegion = config.getString(nameProp);
    } else if (config.hasPath(dfltProp)) {
      endpointRegion = config.getString(dfltProp);
    }
    return endpointRegion;
  }

  /**
   * Create a new instance of an AWS client of the specified type. The name of the config
   * block will be based on the package for the class name. For example, if requesting an
   * instance of {@code com.amazonaws.services.ec2.AmazonEC2} the config name used will
   * be {@code ec2}.
   *
   * @param cls
   *     Class for the AWS client type to create, e.g. {@code AmazonEC2.class}.
   * @return
   *     AWS client instance.
   */
  public <T> T newInstance(Class<T> cls) {
    return newInstance(getDefaultName(cls), cls);
  }

  /**
   * Create a new instance of an AWS client of the specified type.
   *
   * @param name
   *     Name of the client. This is used to load config settings specific to the name.
   * @param cls
   *     Class for the AWS client type to create, e.g. {@code AmazonEC2.class}.
   * @return
   *     AWS client instance.
   */
  public <T> T newInstance(String name, Class<T> cls) {
    return newInstance(name, cls, null);
  }

  /**
   * Create a new instance of an AWS client. The name of the config
   * block will be based on the package for the class name. For example, if requesting an
   * instance of {@code com.amazonaws.services.ec2.AmazonEC2} the config name used will
   * be {@code ec2}.
   *
   * @param cls
   *     Class for the AWS client type to create, e.g. {@code AmazonEC2.class}.
   * @param accountId
   *     The AWS account id to use when assuming to a role. If null, then the account
   *     id should be specified directly in the role-arn setting or leave out the setting
   *     to use the default credentials provider.
   * @return
   *     AWS client instance.
   */
  public <T> T newInstance(Class<T> cls, String accountId) {
    return newInstance(getDefaultName(cls), cls, accountId);
  }

  /**
   * Create a new instance of an AWS client. This method will always create a new instance.
   * If you want to create or reuse an existing instance, then see
   * {@link #getInstance(String, Class, String)}.
   *
   * @param name
   *     Name of the client. This is used to load config settings specific to the name.
   * @param cls
   *     Class for the AWS client type to create, e.g. {@code AmazonEC2.class}.
   * @param accountId
   *     The AWS account id to use when assuming to a role. If null, then the account
   *     id should be specified directly in the role-arn setting or leave out the setting
   *     to use the default credentials provider.
   * @return
   *     AWS client instance.
   */
  @SuppressWarnings("unchecked")
  public <T> T newInstance(String name, Class<T> cls, String accountId) {
    try {
      final Class<?> clientCls = getClientClass(cls);
      Method builderMethod = clientCls.getMethod("builder");
      return (T) ((AwsClientBuilder) builderMethod.invoke(null))
          .withCredentials(createCredentialsProvider(name, accountId))
          .withClientConfiguration(createClientConfig(name))
          .withRegion(chooseRegion(name, cls))
          .build();
    } catch (Exception e) {
      throw new RuntimeException("failed to create instance of " + cls.getName(), e);
    }
  }

  /**
   * Get a shared instance of an AWS client of the specified type. The name of the config
   * block will be based on the package for the class name. For example, if requesting an
   * instance of {@code com.amazonaws.services.ec2.AmazonEC2} the config name used will
   * be {@code ec2}.
   *
   * @param cls
   *     Class for the AWS client type to create, e.g. {@code AmazonEC2.class}.
   * @return
   *     AWS client instance.
   */
  public <T> T getInstance(Class<T> cls) {
    return getInstance(getDefaultName(cls), cls);
  }

  /**
   * Get a shared instance of an AWS client.
   *
   * @param name
   *     Name of the client. This is used to load config settings specific to the name.
   * @param cls
   *     Class for the AWS client type to create, e.g. {@code AmazonEC2.class}.
   * @return
   *     AWS client instance.
   */
  public <T> T getInstance(String name, Class<T> cls) {
    return getInstance(name, cls, null);
  }

  /**
   * Get a shared instance of an AWS client. The name of the config
   * block will be based on the package for the class name. For example, if requesting an
   * instance of {@code com.amazonaws.services.ec2.AmazonEC2} the config name used will
   * be {@code ec2}.
   *
   * @param cls
   *     Class for the AWS client type to create, e.g. {@code AmazonEC2.class}.
   * @param accountId
   *     The AWS account id to use when assuming to a role. If null, then the account
   *     id should be specified directly in the role-arn setting or leave out the setting
   *     to use the default credentials provider.
   * @return
   *     AWS client instance.
   */
  public <T> T getInstance(Class<T> cls, String accountId) {
    return getInstance(getDefaultName(cls), cls, accountId);
  }

  /**
   * Get a shared instance of an AWS client.
   *
   * @param name
   *     Name of the client. This is used to load config settings specific to the name.
   * @param cls
   *     Class for the AWS client type to create, e.g. {@code AmazonEC2.class}.
   * @param accountId
   *     The AWS account id to use when assuming to a role. If null, then the account
   *     id should be specified directly in the role-arn setting or leave out the setting
   *     to use the default credentials provider.
   * @return
   *     AWS client instance.
   */
  @SuppressWarnings("unchecked")
  public <T> T getInstance(String name, Class<T> cls, String accountId) {
    try {
      final Class<?> clientCls = getClientClass(cls);
      final String key = name + ":" + clientCls.getName() + ":" + accountId;
      return (T) clients.computeIfAbsent(key,
          k -> (AmazonWebServiceClient) newInstance(name, clientCls, accountId));
    } catch (Exception e) {
      throw new RuntimeException("failed to get instance of " + cls.getName(), e);
    }
  }

  /**
   * Cleanup resources used by shared clients.
   */
  @Override public void close() throws Exception {
    clients.forEach((k, client) -> client.shutdown());
  }
}
