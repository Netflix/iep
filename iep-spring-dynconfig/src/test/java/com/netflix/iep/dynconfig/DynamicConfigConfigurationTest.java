/*
 * Copyright 2014-2026 Netflix, Inc.
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
package com.netflix.iep.dynconfig;

import com.netflix.iep.config.ConfigManager;
import com.netflix.iep.config.DynamicConfigManager;
import com.netflix.iep.service.State;
import com.netflix.spectator.ipc.http.HttpClient;
import com.netflix.spectator.ipc.http.HttpRequestBuilder;
import com.netflix.spectator.ipc.http.HttpResponse;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class DynamicConfigConfigurationTest {

  @Test
  public void dynamicConfigManager() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(DynamicConfigConfiguration.class);
      context.refresh();
      context.start();
      DynamicConfigManager manager = context.getBean(DynamicConfigManager.class);
      Assert.assertSame(ConfigManager.dynamicConfigManager(), manager);
    }
  }

  @Test
  public void dynamicConfigService() throws Exception {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(DynamicConfigConfiguration.class);
      context.refresh();
      context.start();
      DynamicConfigService service = context.getBean(DynamicConfigService.class);
      Assert.assertEquals(State.RUNNING, service.state());
    }
  }

  @Test
  public void pollingWithoutSslFactory() {
    checkPolling(null, null);
  }

  @Test
  public void pollingWithSslFactory() throws Exception {
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, null, null);
    SSLSocketFactory factory = sslContext.getSocketFactory();
    SSLSocketFactory otherFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    checkPolling(factory, otherFactory);
  }

  @Test
  public void pollingIgnoresUnrelatedSslFactory() {
    checkPolling(null, (SSLSocketFactory) SSLSocketFactory.getDefault());
  }

  private void checkPolling(SSLSocketFactory factory, SSLSocketFactory otherFactory) {
    RecordingRequest request = new RecordingRequest();
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(DynamicConfigConfiguration.class);
      context.registerBean(HttpClient.class, () -> uri -> request);
      if (factory != null) {
        context.registerBean("iep.archaius", SSLSocketFactory.class, () -> factory);
      }
      if (otherFactory != null) {
        context.registerBean("other.client", SSLSocketFactory.class, () -> otherFactory);
      }
      context.refresh();
      DynamicConfigService service = context.getBean(DynamicConfigService.class);
      Assert.assertTrue(service.update());
      Assert.assertSame(factory, request.sslFactory);
      Assert.assertEquals(factory != null, request.factoryConfigured);
      Assert.assertEquals("remote", ConfigManager.dynamicConfigManager().get()
          .getString("iep.test.which-config"));
    } finally {
      ConfigManager.dynamicConfigManager().setOverrideConfig(ConfigFactory.empty());
    }
  }

  private static class RecordingRequest extends HttpRequestBuilder {

    private SSLSocketFactory sslFactory;
    private boolean factoryConfigured;

    RecordingRequest() {
      super(HttpClient.DEFAULT_LOGGER, URI.create("https://localhost/props"));
    }

    @Override public HttpRequestBuilder withSSLSocketFactory(SSLSocketFactory factory) {
      sslFactory = factory;
      factoryConfigured = true;
      return super.withSSLSocketFactory(factory);
    }

    @Override public HttpResponse send() {
      byte[] body = "iep.test.which-config=remote\n".getBytes(StandardCharsets.UTF_8);
      return new HttpResponse(200, Collections.emptyMap(), body);
    }
  }
}
