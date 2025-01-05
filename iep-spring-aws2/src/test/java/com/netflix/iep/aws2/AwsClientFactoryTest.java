/*
 * Copyright 2014-2025 Netflix, Inc.
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
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpService;
import software.amazon.awssdk.http.apache.ApacheSdkHttpService;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2AsyncClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesRequest;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.utils.AttributeMap;


import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@RunWith(JUnit4.class)
public class AwsClientFactoryTest {

  private final Config config = ConfigFactory.load("aws-client-factory");

  @Test
  public void getDefaultName() {
    AwsClientFactory factory = new AwsClientFactory(config);
    Assert.assertEquals("ec2", factory.getDefaultName(Ec2Client.class));
    Assert.assertEquals("ec2", factory.getDefaultName(DescribeAddressesRequest.class));
  }

  @Test
  public void getDefaultNameAsync() {
    AwsClientFactory factory = new AwsClientFactory(config);
    Assert.assertEquals("ec2", factory.getDefaultName(Ec2AsyncClient.class));
  }

  private String getUserAgentPrefix(ClientOverrideConfiguration settings) {
    return settings.advancedOption(SdkAdvancedClientOption.USER_AGENT_PREFIX).orElse(null);
  }

  private String getUserAgentSuffix(ClientOverrideConfiguration settings) {
    return settings.advancedOption(SdkAdvancedClientOption.USER_AGENT_SUFFIX).orElse(null);
  }

  @Test
  public void createClientConfig() {
    AwsClientFactory factory = new AwsClientFactory(config);
    Config cfg = factory.getConfig(null, Ec2Client.class).getConfig("client");
    ClientOverrideConfiguration settings = factory.createClientConfig(cfg);
    Assert.assertEquals("default", getUserAgentPrefix(settings));
  }

  @Test
  public void createClientConfigOverride() {
    AwsClientFactory factory = new AwsClientFactory(config);
    Config cfg = factory.getConfig("ec2-test", Ec2Client.class).getConfig("client");
    ClientOverrideConfiguration settings = factory.createClientConfig(cfg);
    Assert.assertEquals("ignored-defaults", getUserAgentPrefix(settings));
  }

  @Test
  public void createClientConfigOverrideWithDefaults() {
    AwsClientFactory factory = new AwsClientFactory(config);
    Config cfg = factory.getConfig("ec2-test-default", Ec2Client.class).getConfig("client");
    ClientOverrideConfiguration settings = factory.createClientConfig(cfg);
    Assert.assertEquals("override-defaults", getUserAgentPrefix(settings));
  }

  private SdkHttpService httpService() {
    return new ApacheSdkHttpService();
  }

  @Test
  public void createCredentialsProvider() {
    AwsClientFactory factory = new AwsClientFactory(config);
    Config cfg = factory.getConfig(null, Ec2Client.class).getConfig("credentials");
    AwsCredentialsProvider creds = factory.createCredentialsProvider(cfg, null, httpService());
    Assert.assertTrue(creds instanceof DefaultCredentialsProvider);
  }

  // Note: this is potentially fragile as it looks at private fields in the class. It is
  // just an additional sanity check so the assertions can be commented out if they break.
  @SuppressWarnings("unchecked")
  private AssumeRoleRequest getRequest(AwsCredentialsProvider creds) throws Exception {
    Class<?> cls = StsAssumeRoleCredentialsProvider.class;
    Field f = cls.getDeclaredField("assumeRoleRequestSupplier");
    f.setAccessible(true);
    return ((Supplier<AssumeRoleRequest>) f.get(creds)).get();
  }

  @Test
  public void createCredentialsProviderOverride() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    Config cfg = factory.getConfig("ec2-test", Ec2Client.class).getConfig("credentials");
    AwsCredentialsProvider creds = factory.createCredentialsProvider(cfg, null, httpService());
    Assert.assertTrue(creds instanceof StsAssumeRoleCredentialsProvider);
    Assert.assertEquals("arn:aws:iam::1234567890:role/IepTest", getRequest(creds).roleArn());
    Assert.assertEquals("iep", getRequest(creds).roleSessionName());
  }

  @Test
  public void createCredentialsProviderForAccount() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    Config cfg = factory.getConfig("ec2-account", Ec2Client.class).getConfig("credentials");
    AwsCredentialsProvider creds = factory.createCredentialsProvider(cfg, "123", httpService());
    Assert.assertTrue(creds instanceof StsAssumeRoleCredentialsProvider);
    Assert.assertEquals("arn:aws:iam::123:role/IepTest", getRequest(creds).roleArn());
    Assert.assertEquals("ieptest", getRequest(creds).roleSessionName());
  }

  @Test(expected = IllegalArgumentException.class)
  public void createCredentialsProviderForAccountNull() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    Config cfg = factory.getConfig("ec2-account", Ec2Client.class).getConfig("credentials");
    factory.createCredentialsProvider(cfg, null, httpService());
  }

  @Test
  public void createCredentialsProviderForAccountIgnored() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    Config cfg = factory.getConfig("ec2-test", Ec2Client.class).getConfig("credentials");
    AwsCredentialsProvider creds = factory.createCredentialsProvider(cfg, "123", httpService());
    Assert.assertTrue(creds instanceof StsAssumeRoleCredentialsProvider);
    Assert.assertEquals("arn:aws:iam::1234567890:role/IepTest", getRequest(creds).roleArn());
    Assert.assertEquals("iep", getRequest(creds).roleSessionName());
  }

  @Test
  public void newInstanceInterface() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    Ec2Client ec2 = factory.newInstance(Ec2Client.class);
    Assert.assertNotNull(ec2);
  }

  @Test(expected = RuntimeException.class)
  public void newInstanceBadClass() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    factory.newInstance(AwsClientFactory.class);
  }

  @Test
  public void newInstanceName() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    Ec2Client ec2 = factory.newInstance("ec2-test", Ec2Client.class);
    Assert.assertNotNull(ec2);
  }

  @Test
  public void newInstanceRegion() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    Ec2Client ec2 = factory.newInstance("ec2-test", Ec2Client.class, "123", Optional.of(Region.of("us-east-1")));
    Assert.assertNotNull(ec2);
  }

  @Test
  public void newInstanceInterfaceAsync() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    Ec2AsyncClient ec2 = factory.newInstance(Ec2AsyncClient.class);
    Assert.assertNotNull(ec2);
  }

  @Test
  public void newInstanceNameAsync() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    Ec2AsyncClient ec2 = factory.newInstance("ec2-test", Ec2AsyncClient.class);
    Assert.assertNotNull(ec2);
  }

  @Test
  public void getInstanceInterface() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    Ec2Client ec2 = factory.getInstance(Ec2Client.class);
    Assert.assertNotNull(ec2);
    Assert.assertSame(ec2, factory.getInstance(Ec2Client.class));
    Assert.assertNotSame(ec2, factory.newInstance(Ec2Client.class));
  }

  @Test
  public void getInstanceClient() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    Ec2Client ec2 = factory.getInstance(Ec2Client.class);
    Assert.assertNotNull(ec2);
    Assert.assertSame(ec2, factory.getInstance(Ec2Client.class));
  }

  @Test(expected = RuntimeException.class)
  public void getInstanceBadClass() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    factory.getInstance(AwsClientFactory.class);
  }

  @Test
  public void getInstanceName() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    Ec2Client ec2 = factory.getInstance("ec2-test", Ec2Client.class);
    Assert.assertNotNull(ec2);
    Assert.assertSame(ec2, factory.getInstance("ec2-test", Ec2Client.class));
    Assert.assertNotSame(ec2, factory.getInstance(Ec2Client.class));
  }

  @Test
  public void getInstanceRegion() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    Ec2Client ec2 = factory.getInstance("ec2-test", Ec2Client.class, "123", Optional.of(Region.of("us-east-1")));
    Assert.assertNotNull(ec2);
    Assert.assertSame(ec2, factory.getInstance("ec2-test", Ec2Client.class, "123", Optional.of(Region.of("us-east-1"))));
    Assert.assertNotSame(ec2, factory.getInstance(Ec2Client.class));
  }

  @Test
  public void closeClients() throws Exception {
    // Verifies the close completes without throwing
    AwsClientFactory factory = new AwsClientFactory(config);
    factory.getInstance(Ec2Client.class);
    factory.close();
  }

  @Test
  public void settingsUserAgentPrefix() {
    AwsClientFactory factory = new AwsClientFactory(config);
    Config cfg = factory.getConfig(null, Ec2Client.class).getConfig("client");
    ClientOverrideConfiguration settings = factory.createClientConfig(cfg);
    Assert.assertEquals("default", getUserAgentPrefix(settings));
  }

  @Test
  public void settingsUserAgentSuffix() {
    AwsClientFactory factory = new AwsClientFactory(config);
    Config cfg = factory.getConfig(null, Ec2Client.class).getConfig("client");
    ClientOverrideConfiguration settings = factory.createClientConfig(cfg);
    Assert.assertEquals("suffix", getUserAgentSuffix(settings));
  }

  @Test
  public void settingsHeaders() {
    AwsClientFactory factory = new AwsClientFactory(config);
    Config cfg = factory.getConfig("headers", Ec2Client.class).getConfig("client");
    ClientOverrideConfiguration settings = factory.createClientConfig(cfg);
    Map<String, List<String>> headers = settings.headers();
    Assert.assertEquals(1, headers.size());
    Assert.assertEquals(Collections.singletonList("gzip"), headers.get("Accept-Encoding"));
  }

  @Test
  public void settingsHeadersInvalid() {
    AwsClientFactory factory = new AwsClientFactory(config);
    Config cfg = factory.getConfig("headers-invalid", Ec2Client.class).getConfig("client");
    ClientOverrideConfiguration settings = factory.createClientConfig(cfg);
    Map<String, List<String>> headers = settings.headers();
    Assert.assertTrue(headers.isEmpty());
  }

  @Test
  public void settingsTimeout() {
    AwsClientFactory factory = new AwsClientFactory(config);
    Config cfg = factory.getConfig("timeouts", Ec2Client.class).getConfig("client");
    ClientOverrideConfiguration settings = factory.createClientConfig(cfg);
    Assert.assertEquals(Duration.ofMillis(42000), settings.apiCallAttemptTimeout().get());
    Assert.assertEquals(Duration.ofMillis(13000), settings.apiCallTimeout().get());
    Assert.assertEquals(6, settings.retryStrategy().get().maxAttempts());
  }

  @Test
  public void settingsSdkHttpClient() {
    AwsClientFactory factory = new AwsClientFactory(config);
    Config cfg = factory.getConfig("sdk-http-client", Ec2Client.class).getConfig("client");
    AttributeMap settings = factory.getSdkHttpConfigurationOptions(cfg);
    Assert.assertEquals(Duration.ofMillis(60000), settings.get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT));
    Assert.assertEquals(Duration.ofMillis(120000), settings.get(SdkHttpConfigurationOption.CONNECTION_MAX_IDLE_TIMEOUT));
    Assert.assertEquals(500, settings.get(SdkHttpConfigurationOption.MAX_CONNECTIONS).intValue());
    Assert.assertEquals(true, settings.get(SdkHttpConfigurationOption.REAP_IDLE_CONNECTIONS));
  }

  private boolean isDualstackEnabled(AwsClientFactory factory, Class<?> cls) {
    return factory.getConfig(null, cls).getBoolean("dualstack");
  }

  @Test
  public void dualstack() {
    AwsClientFactory factory = new AwsClientFactory(config);
    Assert.assertTrue(isDualstackEnabled(factory, Ec2Client.class));
    Assert.assertFalse(isDualstackEnabled(factory, StsClient.class));
  }
}
