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
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.ec2.EC2Client;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesRequest;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import java.lang.reflect.Field;
import java.time.Duration;

@RunWith(JUnit4.class)
public class AwsClientFactoryTest {

  private final Config config = ConfigFactory.load("aws-client-factory");

  @Test
  public void getDefaultName() {
    AwsClientFactory factory = new AwsClientFactory(config);
    Assert.assertEquals("ec2", factory.getDefaultName(EC2Client.class));
    Assert.assertEquals("ec2", factory.getDefaultName(DescribeAddressesRequest.class));
  }

  @Test
  public void createClientConfig() {
    AwsClientFactory factory = new AwsClientFactory(config);
    ClientOverrideConfiguration settings = factory.createClientConfig(null);
    Assert.assertEquals(true, settings.gzipEnabled());
  }

  @Test
  public void createClientConfigOverride() {
    AwsClientFactory factory = new AwsClientFactory(config);
    ClientOverrideConfiguration settings = factory.createClientConfig("ec2-test");
    Assert.assertEquals(false, settings.gzipEnabled());
  }

  @Test
  public void createClientConfigOverrideWithDefaults() {
    AwsClientFactory factory = new AwsClientFactory(config);
    ClientOverrideConfiguration settings = factory.createClientConfig("ec2-test-default");
    Assert.assertEquals(false, settings.gzipEnabled());
  }

  @Test
  public void createCredentialsProvider() {
    AwsClientFactory factory = new AwsClientFactory(config);
    AwsCredentialsProvider creds = factory.createCredentialsProvider(null);
    Assert.assertTrue(creds instanceof DefaultCredentialsProvider);
  }

  // Note: this is potentially fragile as it looks at private fields in the class. It is
  // just an additional sanity check so the assertions can be commented out if they break.
  private AssumeRoleRequest getRequest(AwsCredentialsProvider creds) throws Exception {
    Class<?> cls = StsAssumeRoleCredentialsProvider.class;
    Field f = cls.getDeclaredField("assumeRoleRequest");
    f.setAccessible(true);
    return (AssumeRoleRequest) f.get(creds);
  }

  @Test
  public void createCredentialsProviderOverride() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    AwsCredentialsProvider creds = factory.createCredentialsProvider("ec2-test");
    Assert.assertTrue(creds instanceof StsAssumeRoleCredentialsProvider);
    Assert.assertEquals("arn:aws:iam::1234567890:role/IepTest", getRequest(creds).roleArn());
    Assert.assertEquals("iep", getRequest(creds).roleSessionName());
  }

  @Test
  public void newInstanceInterface() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    EC2Client ec2 = factory.newInstance(EC2Client.class);
    Assert.assertNotNull(ec2);
  }

  @Test
  public void newInstanceClient() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    EC2Client ec2 = factory.newInstance(EC2Client.class);
    Assert.assertNotNull(ec2);
  }

  @Test
  public void newInstanceName() throws Exception {
    AwsClientFactory factory = new AwsClientFactory(config);
    EC2Client ec2 = factory.newInstance("ec2-test", EC2Client.class);
    Assert.assertNotNull(ec2);
  }

  @Test
  public void settingsBooleanTrue() {
    AwsClientFactory factory = new AwsClientFactory(config);
    ClientOverrideConfiguration settings = factory.createClientConfig("boolean-true");
    Assert.assertEquals(true, settings.gzipEnabled());
  }

  @Test
  public void settingsBooleanFalse() {
    AwsClientFactory factory = new AwsClientFactory(config);
    ClientOverrideConfiguration settings = factory.createClientConfig("boolean-false");
    Assert.assertEquals(false, settings.gzipEnabled());
  }

  // TODO: timeouts are no longer settable on the override configuration
  /*@Test
  public void settingsTimeout() {
    AwsClientFactory factory = new AwsClientFactory(config);
    ClientOverrideConfiguration settings = factory.createClientConfig("timeouts");
    Assert.assertEquals(Duration.ofMillis(42000), settings.httpRequestTimeout());
    Assert.assertEquals(Duration.ofMillis(13000), settings.totalExecutionTimeout());
  }*/
}
