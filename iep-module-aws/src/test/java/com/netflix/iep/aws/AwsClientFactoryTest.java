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
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeAddressesRequest;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Field;

@RunWith(JUnit4.class)
public class AwsClientFactoryTest {

  private final Config config = ConfigFactory.load("aws-client-factory");

  @Test
  public void getDefaultName() {
    AwsClientFactory factory = new AwsClientFactory(config);
    Assert.assertEquals("ec2", factory.getDefaultName(AmazonEC2.class));
    Assert.assertEquals("ec2", factory.getDefaultName(DescribeAddressesRequest.class));
  }

  @Test
  public void createClientConfig() {
    AwsClientFactory factory = new AwsClientFactory(config);
    ClientConfiguration settings = factory.createClientConfig(null);
    Assert.assertEquals(true, settings.useGzip());
    Assert.assertEquals("abc", settings.getUserAgentPrefix());
    Assert.assertEquals("xyz", settings.getUserAgentSuffix());
  }

  @Test
  public void createClientConfigOverride() {
    AwsClientFactory factory = new AwsClientFactory(config);
    ClientConfiguration settings = factory.createClientConfig("ec2-test");
    Assert.assertEquals(false, settings.useGzip());
  }

  @Test
  public void createClientConfigOverrideWithDefaults() {
    AwsClientFactory factory = new AwsClientFactory(config);
    ClientConfiguration settings = factory.createClientConfig("ec2-test-default");
    Assert.assertEquals(false, settings.useGzip());
    Assert.assertEquals("abc", settings.getUserAgentPrefix());
    Assert.assertEquals("xyz", settings.getUserAgentSuffix());
  }

  @Test
  public void createCredentialsProvider() {
    AwsClientFactory factory = new AwsClientFactory(config);
    AWSCredentialsProvider creds = factory.createCredentialsProvider(null, null);
    Assert.assertTrue(creds instanceof DefaultAWSCredentialsProviderChain);
  }

  // Note: this is potentially fragile as it looks at private fields in the class. It is
  // just an additional sanity check so the assertions can be commented out if they break.
  private String getField(AWSCredentialsProvider creds, String field) throws Exception {
    Class<?> cls = STSAssumeRoleSessionCredentialsProvider.class;
    Field f = cls.getDeclaredField(field);
    f.setAccessible(true);
    return (String) f.get(creds);
  }

  @Test
  public void createCredentialsProviderOverride() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    AWSCredentialsProvider creds = factory.createCredentialsProvider("ec2-test", null);
    Assert.assertTrue(creds instanceof STSAssumeRoleSessionCredentialsProvider);
    Assert.assertEquals("arn:aws:iam::1234567890:role/IepTest", getField(creds, "roleArn"));
    Assert.assertEquals("iep", getField(creds, "roleSessionName"));
  }

  @Test
  public void createCredentialsProviderForAccount() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    AWSCredentialsProvider creds = factory.createCredentialsProvider("ec2-account", "123");
    Assert.assertTrue(creds instanceof STSAssumeRoleSessionCredentialsProvider);
    Assert.assertEquals("arn:aws:iam::123:role/IepTest", getField(creds, "roleArn"));
    Assert.assertEquals("iep", getField(creds, "roleSessionName"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void createCredentialsProviderForAccountNull() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    factory.createCredentialsProvider("ec2-account", null);
  }

  @Test
  public void createCredentialsProviderForAccountIgnored() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    AWSCredentialsProvider creds = factory.createCredentialsProvider("ec2-test", "123");
    Assert.assertTrue(creds instanceof STSAssumeRoleSessionCredentialsProvider);
    Assert.assertEquals("arn:aws:iam::1234567890:role/IepTest", getField(creds, "roleArn"));
    Assert.assertEquals("iep", getField(creds, "roleSessionName"));
  }

  @Test
  public void newInstanceInterface() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    AmazonEC2 ec2 = factory.newInstance(AmazonEC2.class);
    Assert.assertNotNull(ec2);
  }

  @Test
  public void newInstanceClient() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    AmazonEC2Client ec2 = factory.newInstance(AmazonEC2Client.class);
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
    AmazonEC2 ec2 = factory.newInstance("ec2-test", AmazonEC2.class);
    Assert.assertNotNull(ec2);
  }

  @Test
  public void getInstanceInterface() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    AmazonEC2 ec2 = factory.getInstance(AmazonEC2.class);
    Assert.assertNotNull(ec2);
    Assert.assertSame(ec2, factory.getInstance(AmazonEC2.class));
    Assert.assertNotSame(ec2, factory.newInstance(AmazonEC2.class));
  }

  @Test
  public void getInstanceClient() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    AmazonEC2Client ec2 = factory.getInstance(AmazonEC2Client.class);
    Assert.assertNotNull(ec2);
    Assert.assertSame(ec2, factory.getInstance(AmazonEC2Client.class));
  }

  @Test(expected = RuntimeException.class)
  public void getInstanceBadClass() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    factory.getInstance(AwsClientFactory.class);
  }

  @Test
  public void getInstanceName() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    AmazonEC2 ec2 = factory.getInstance("ec2-test", AmazonEC2.class);
    Assert.assertNotNull(ec2);
    Assert.assertSame(ec2, factory.getInstance("ec2-test", AmazonEC2.class));
    Assert.assertNotSame(ec2, factory.getInstance(AmazonEC2.class));
  }

  @Test
  public void closeClients() throws Exception {
    // Verifies the close completes without throwing
    AwsClientFactory factory = new AwsClientFactory(config);
    factory.getInstance(AmazonEC2.class);
    factory.close();
  }

  @Test
  public void settingsBooleanTrue() {
    AwsClientFactory factory = new AwsClientFactory(config);
    ClientConfiguration settings = factory.createClientConfig("boolean-true");
    Assert.assertEquals(true, settings.useReaper());
    Assert.assertEquals(true, settings.useTcpKeepAlive());
    Assert.assertEquals(true, settings.useGzip());
    Assert.assertEquals(true, settings.useThrottledRetries());
  }

  @Test
  public void settingsBooleanFalse() {
    AwsClientFactory factory = new AwsClientFactory(config);
    ClientConfiguration settings = factory.createClientConfig("boolean-false");
    Assert.assertEquals(false, settings.useReaper());
    Assert.assertEquals(false, settings.useTcpKeepAlive());
    Assert.assertEquals(false, settings.useGzip());
    Assert.assertEquals(false, settings.useThrottledRetries());
  }

  @Test
  public void settingsTimeout() {
    AwsClientFactory factory = new AwsClientFactory(config);
    ClientConfiguration settings = factory.createClientConfig("timeouts");
    Assert.assertEquals(51000, settings.getConnectionTimeout());
    Assert.assertEquals(42000, settings.getSocketTimeout());
    Assert.assertEquals(13000, settings.getClientExecutionTimeout());
  }

  @Test
  public void settingsTTL() {
    AwsClientFactory factory = new AwsClientFactory(config);
    ClientConfiguration settings = factory.createClientConfig("ttl");
    Assert.assertEquals(42000, settings.getConnectionTTL());
    Assert.assertEquals(51000, settings.getConnectionMaxIdleMillis());
  }

  @Test
  public void settingsIntegers() {
    AwsClientFactory factory = new AwsClientFactory(config);
    ClientConfiguration settings = factory.createClientConfig("integers");
    Assert.assertEquals(27, settings.getMaxConnections());
    Assert.assertEquals(3, settings.getMaxErrorRetry());
  }

  @Test
  public void settingsProxy() {
    AwsClientFactory factory = new AwsClientFactory(config);
    ClientConfiguration settings = factory.createClientConfig("proxy");
    Assert.assertEquals(12345, settings.getProxyPort());
    Assert.assertEquals("host", settings.getProxyHost());
    Assert.assertEquals("domain", settings.getProxyDomain());
    Assert.assertEquals("workstation", settings.getProxyWorkstation());
    Assert.assertEquals("username", settings.getProxyUsername());
    Assert.assertEquals("password", settings.getProxyPassword());
  }

}
