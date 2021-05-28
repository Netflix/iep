/*
 * Copyright 2014-2021 Netflix, Inc.
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
package com.netflix.iep.aws2;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.http.SdkHttpService;
import software.amazon.awssdk.http.async.SdkAsyncHttpService;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.utils.SdkAutoCloseable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Factory for creating instances of AWS clients.
 */
@Singleton
public class AwsClientFactory implements AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(AwsClientFactory.class);

  private final ConcurrentHashMap<String, SdkAutoCloseable> clients = new ConcurrentHashMap<>();

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
    final String prefix = "software.amazon.awssdk.services.";
    String pkg = cls.getPackage().getName();
    return pkg.startsWith(prefix) ? firstOnly(pkg.substring(prefix.length())) : null;
  }

  private void setIfPresent(Config cfg, String key, Consumer<Duration> setter) {
    if (cfg.hasPath(key)) {
      setter.accept(cfg.getDuration(key));
    }
  }

  private void setIfPresent(
      Config cfg,
      String key,
      SdkAdvancedClientOption<String> option,
      ClientOverrideConfiguration.Builder builder) {
    if (cfg.hasPath(key)) {
      builder.putAdvancedOption(option, cfg.getString(key));
    }
  }

  ClientOverrideConfiguration createClientConfig(String name) {
    final Config cfg = getConfig(name, "client");
    ClientOverrideConfiguration.Builder builder = ClientOverrideConfiguration.builder();

    setIfPresent(cfg, "api-call-timeout", builder::apiCallTimeout);
    setIfPresent(cfg, "api-call-attempt-timeout", builder::apiCallAttemptTimeout);

    setIfPresent(cfg, "user-agent-prefix", SdkAdvancedClientOption.USER_AGENT_PREFIX, builder);
    setIfPresent(cfg, "user-agent-suffix", SdkAdvancedClientOption.USER_AGENT_SUFFIX, builder);

    if (cfg.hasPath("headers")) {
      for (String header : cfg.getStringList("headers")) {
        String[] parts = header.split(":", 2);
        if (parts.length == 2) {
          builder.putHeader(parts[0].trim(), parts[1].trim());
        } else {
          LOGGER.warn("ignoring invalid header string: '{}'", header);
        }
      }
    }

    return builder.build();
  }

  private Config getConfig(String name, String suffix) {
    final String cfgPrefix = "netflix.iep.aws";
    return (name != null && config.hasPath(cfgPrefix + "." + name + "." + suffix))
        ? config.getConfig(cfgPrefix + "." + name + "." + suffix)
        : config.getConfig(cfgPrefix + ".default." + suffix);
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

  private AwsCredentialsProvider createAssumeRoleProvider(
      Config cfg, String accountId, AwsCredentialsProvider p, SdkHttpService service) {
    final String arn = createRoleArn(cfg.getString("role-arn"), accountId);
    final String name = cfg.getString("role-session-name");
    final StsClient stsClient = StsClient.builder()
        .credentialsProvider(p)
        .region(Region.of(region))
        .httpClientBuilder(service.createHttpClientBuilder())
        .build();
    final AssumeRoleRequest request = AssumeRoleRequest.builder()
        .roleArn(arn)
        .roleSessionName(name)
        .build();
    return StsAssumeRoleCredentialsProvider.builder()
        .stsClient(stsClient)
        .refreshRequest(request)
        .build();
  }

  AwsCredentialsProvider createCredentialsProvider(
      String name, String accountId, SdkHttpService service) {
    final AwsCredentialsProvider dflt = DefaultCredentialsProvider.builder()
        .asyncCredentialUpdateEnabled(true)
        .build();
    final Config cfg = getConfig(name, "credentials");
    if (cfg.hasPath("role-arn")) {
      return createAssumeRoleProvider(cfg, accountId, dflt, service);
    } else {
      if (accountId != null) {
        LOGGER.warn("requested account, {}, ignored, no role ARN configured", accountId);
      }
      return dflt;
    }
  }

  private Region chooseRegion(String name, Class<?> cls) {
    final String nameProp = "netflix.iep.aws." + name + ".region";
    final String service = getDefaultName(cls);
    final String dfltProp = "netflix.iep.aws.endpoint." + service + "." + region;
    String endpointRegion = region;
    if (config.hasPath(nameProp)) {
      endpointRegion = config.getString(nameProp);
    } else if (config.hasPath(dfltProp)) {
      endpointRegion = config.getString(dfltProp);
    }
    return Region.of(endpointRegion);
  }

  private SdkHttpService createSyncHttpService(String name) {
    Config clientConfig = getConfig(name, "client");
    if (clientConfig.hasPath("sync-http-impl")) {
      String clsName = clientConfig.getString("sync-http-impl");
      try {
        Class<?> clientClass = Class.forName(clsName);
        return (SdkHttpService) clientClass.getConstructor().newInstance();
      } catch (Exception e) {
        throw new RuntimeException("failed to create instance of " + clsName, e);
      }
    } else {
      Iterator<SdkHttpService> services = ServiceLoader
          .load(SdkHttpService.class)
          .iterator();
      if (services.hasNext()) {
        return services.next();
      } else {
        throw new IllegalStateException("could not find SdkHttpService on classpath, " +
            "set `sync-http-impl` to specify the implementation to use");
      }
    }
  }

  private SdkAsyncHttpService createAsyncHttpService(String name) {
    Config clientConfig = getConfig(name, "client");
    if (clientConfig.hasPath("async-http-impl")) {
      String clsName = clientConfig.getString("async-http-impl");
      try {
        Class<?> clientClass = Class.forName(clsName);
        return (SdkAsyncHttpService) clientClass.getConstructor().newInstance();
      } catch (Exception e) {
        throw new RuntimeException("failed to create instance of " + clsName, e);
      }
    } else {
      Iterator<SdkAsyncHttpService> services = ServiceLoader
          .load(SdkAsyncHttpService.class)
          .iterator();
      if (services.hasNext()) {
        return services.next();
      } else {
        throw new IllegalStateException("could not find SdkAsyncHttpService on classpath, " +
            "set `async-http-impl` to specify the implementation to use");
      }
    }
  }

  /**
   * Create a new instance of an AWS client of the specified type. The name of the config
   * block will be based on the package for the class name. For example, if requesting an
   * instance of {@code software.amazon.awssdk.services.ec2.Ec2Client} the config name used
   * will be {@code ec2}.
   *
   * @param cls
   *     Class for the AWS client type to create, e.g. {@code Ec2Client.class}.
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
   *     Class for the AWS client type to create, e.g. {@code Ec2Client.class}.
   * @return
   *     AWS client instance.
   */
  public <T> T newInstance(String name, Class<T> cls) {
    return newInstance(name, cls, null);
  }

  /**
   * Create a new instance of an AWS client. The name of the config
   * block will be based on the package for the class name. For example, if requesting an
   * instance of {@code software.amazon.awssdk.services.ec2.Ec2Client} the config name used
   * will be {@code ec2}.
   *
   * @param cls
   *     Class for the AWS client type to create, e.g. {@code Ec2Client.class}.
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
   *     Class for the AWS client type to create, e.g. {@code Ec2Client.class}.
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
      SdkHttpService service = createSyncHttpService(name);
      Method builderMethod = cls.getMethod("builder");
      AwsClientBuilder<?, ?> builder = ((AwsClientBuilder<?, ?>) builderMethod.invoke(null))
          .credentialsProvider(createCredentialsProvider(name, accountId, service))
          .region(chooseRegion(name, cls))
          .overrideConfiguration(createClientConfig(name));

      if (builder instanceof AwsSyncClientBuilder<?, ?>) {
        ((AwsSyncClientBuilder<?, ?>) builder)
            .httpClientBuilder(service.createHttpClientBuilder());
      } else if (builder instanceof AwsAsyncClientBuilder<?, ?>) {
        SdkAsyncHttpService asyncService = createAsyncHttpService(name);
        ((AwsAsyncClientBuilder<?, ?>) builder)
            .httpClientBuilder(asyncService.createAsyncHttpClientFactory());
      }

      return (T) builder.build();
    } catch (Exception e) {
      throw new RuntimeException("failed to create instance of " + cls.getName(), e);
    }
  }

  /**
   * Get a shared instance of an AWS client of the specified type. The name of the config
   * block will be based on the package for the class name. For example, if requesting an
   * instance of {@code software.amazon.awssdk.services.ec2.Ec2Client} the config name used
   * will be {@code ec2}.
   *
   * @param cls
   *     Class for the AWS client type to create, e.g. {@code Ec2Client.class}.
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
   *     Class for the AWS client type to create, e.g. {@code Ec2Client.class}.
   * @return
   *     AWS client instance.
   */
  public <T> T getInstance(String name, Class<T> cls) {
    return getInstance(name, cls, null);
  }

  /**
   * Get a shared instance of an AWS client. The name of the config
   * block will be based on the package for the class name. For example, if requesting an
   * instance of {@code software.amazon.awssdk.services.ec2.Ec2Client} the config name used
   * will be {@code ec2}.
   *
   * @param cls
   *     Class for the AWS client type to create, e.g. {@code Ec2Client.class}.
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
   *     Class for the AWS client type to create, e.g. {@code Ec2Client.class}.
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
      final String key = name + ":" + cls.getName() + ":" + accountId;
      return (T) clients.computeIfAbsent(key,
          k -> (SdkAutoCloseable) newInstance(name, cls, accountId));
    } catch (Exception e) {
      throw new RuntimeException("failed to get instance of " + cls.getName(), e);
    }
  }

  /**
   * Cleanup resources used by shared clients.
   */
  @Override public void close() throws Exception {
    clients.values().forEach(SdkAutoCloseable::close);
  }
}
