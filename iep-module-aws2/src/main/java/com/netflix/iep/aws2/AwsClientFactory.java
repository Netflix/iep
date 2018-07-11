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
package com.netflix.iep.aws2;

import com.typesafe.config.Config;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.core.client.builder.ClientHttpConfiguration;
import software.amazon.awssdk.core.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.STSClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Factory for creating instances of AWS clients.
 */
@Singleton
public class AwsClientFactory {

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

  ClientOverrideConfiguration createClientConfig(String name) {
    final Config cfg = getConfig(name, "client");
    final ClientOverrideConfiguration.Builder settings = ClientOverrideConfiguration.builder();

    // Helpers
    Function<String, Long> getMillis = k -> cfg.getDuration(k, TimeUnit.MILLISECONDS);
    Function<String, Duration> getTimeout = cfg::getDuration;

    // Typically use the defaults
    setIfPresent(cfg, "use-gzip", cfg::getBoolean, settings::gzipEnabled);
    return settings.build();
  }

  private AwsCredentialsProvider createAssumeRoleProvider(Config cfg, AwsCredentialsProvider p) {
    final String arn = cfg.getString("role-arn");
    final String name = cfg.getString("role-session-name");
    final STSClient stsClient = STSClient.builder()
        .credentialsProvider(p)
        .region(Region.of(region))
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

  AwsCredentialsProvider createCredentialsProvider(String name) {
    final AwsCredentialsProvider dflt = DefaultCredentialsProvider.builder()
        .asyncCredentialUpdateEnabled(true)
        .build();
    final Config cfg = getConfig(name, "credentials");
    if (cfg.hasPath("role-arn")) {
      return createAssumeRoleProvider(cfg, dflt);
    } else {
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

  public <T> T newInstance(Class<T> cls) {
    return newInstance(getDefaultName(cls), cls);
  }

  @SuppressWarnings("unchecked")
  public <T> T newInstance(String name, Class<T> cls) {
    try {
      Method builderMethod = cls.getMethod("builder");
      return (T) ((AwsClientBuilder) builderMethod.invoke(null))
          .credentialsProvider(createCredentialsProvider(name))
          .region(chooseRegion(name, cls))
          .overrideConfiguration(createClientConfig(name))
          .build();
    } catch (Exception e) {
      throw new RuntimeException("failed to create instance of " + cls.getName(), e);
    }
  }
}
