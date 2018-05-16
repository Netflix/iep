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

import com.netflix.appinfo.InstanceInfo;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.config.DefaultSettableConfig;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.timeout.ReadTimeoutException;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import rx.functions.Actions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(JUnit4.class)
public class RxHttpTest {

  private static final String client = "test";
  private static final int retries = 2;

  private static HttpServer server;
  private static int port;

  private static final AtomicInteger statusCode = new AtomicInteger(200);
  private static final AtomicIntegerArray statusCounts = new AtomicIntegerArray(600);

  private static final AtomicInteger redirects = new AtomicInteger(0);

  private static final SettableConfig archaius = new DefaultSettableConfig();
  private static final RxHttp rxHttp = new RxHttp(archaius, null);

  private static void set(String k, String v) {
    archaius.setProperty(k, v);
  }

  private static void ignore(InputStream input) throws IOException {
    try (InputStream in = input) {
      byte[] buf = new byte[1024];
      while (in.read(buf) > 0);
    }
  }

  @BeforeClass
  public static void startServer() throws Exception {

    server = HttpServer.create(new InetSocketAddress(0), 100);
    server.setExecutor(Executors.newFixedThreadPool(10, r -> new Thread(r, "HttpServer")));
    port = server.getAddress().getPort();

    server.createContext("/empty", exchange -> {
      ignore(exchange.getRequestBody());
      int port1 = exchange.getRemoteAddress().getPort();
      exchange.getResponseHeaders().add("X-Test-Port", "" + port1);
      statusCounts.incrementAndGet(statusCode.get());
      exchange.sendResponseHeaders(statusCode.get(), -1L);
      exchange.close();
    });

    server.createContext("/echo", exchange -> {
      Headers headers = exchange.getRequestHeaders();
      int contentLength = Integer.parseInt(headers.getFirst("Content-Length"));
      String contentEnc = headers.getFirst("Content-Encoding");
      if (contentEnc != null) {
        exchange.getResponseHeaders().add("Content-Encoding", contentEnc);
      }

      int code = statusCode.get();
      if (contentLength > 512 && !"gzip".equals(contentEnc)) {
        code = 400;
      }

      statusCounts.incrementAndGet(code);
      exchange.sendResponseHeaders(code, contentLength);
      try (InputStream input = exchange.getRequestBody();
           OutputStream output = exchange.getResponseBody()) {
        byte[] buf = new byte[1024];
        int length;
        while ((length = input.read(buf)) > 0) {
          output.write(buf, 0, length);
        }
      }
      exchange.close();
    });

    server.createContext("/relativeRedirect", exchange -> {
      ignore(exchange.getRequestBody());
      if (redirects.get() <= 0) {
        statusCounts.incrementAndGet(statusCode.get());
        exchange.getResponseHeaders().add("Location", "/empty");
        exchange.sendResponseHeaders(statusCode.get(), -1L);
        exchange.close();
      } else {
        redirects.decrementAndGet();
        statusCounts.incrementAndGet(302);
        exchange.getResponseHeaders().add("Location", "/relativeRedirect");
        exchange.sendResponseHeaders(302, -1L);
        exchange.close();
      }
    });

    server.createContext("/absoluteRedirect", exchange -> {
      String host = "http://" + exchange.getRequestHeaders().getFirst("Host");
      ignore(exchange.getRequestBody());
      if (redirects.get() <= 0) {
        statusCounts.incrementAndGet(302);
        exchange.getResponseHeaders().add("Location", host + "/empty");
        exchange.sendResponseHeaders(302, -1L);
        exchange.close();
      } else {
        redirects.decrementAndGet();
        statusCounts.incrementAndGet(302);
        exchange.getResponseHeaders().add("Location", host + "/absoluteRedirect");
        exchange.sendResponseHeaders(302, -1L);
        exchange.close();
      }
    });

    server.createContext("/notModified", exchange -> {
      ignore(exchange.getRequestBody());
      statusCounts.incrementAndGet(304);
      exchange.sendResponseHeaders(304, -1L);
      exchange.close();
    });

    server.createContext("/redirectNoLocation", exchange -> {
      ignore(exchange.getRequestBody());
      statusCounts.incrementAndGet(302);
      exchange.sendResponseHeaders(302, -1L);
      exchange.close();
    });

    server.createContext("/readTimeout", exchange -> {
      ignore(exchange.getRequestBody());
      statusCounts.incrementAndGet(statusCode.get()); // So we can track retries
      Object lock = new Object();
      try {
        synchronized (lock) {
          lock.wait();
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });

    server.start();

    set(client + ".niws.client.MaxAutoRetriesNextServer", "" + retries);
    set(client + ".niws.client.RetryDelay", "100");
    set(client + ".niws.client.ReadTimeout", "1000");
  }

  @AfterClass
  public static void stopServer() throws Exception {
    rxHttp.close();
    server.stop(0);
  }

  @Before
  public void initProps() {
    set(client + ".niws.client.MaxAutoRetriesNextServer", "" + retries);
    set(client + ".niws.client.RetryDelay", "100");
    set(client + ".niws.client.ConnectTimeout", "1000");
    //set(client + ".niws.client.ReadTimeout", "30000");
  }

  private URI uri(String path) {
    return URI.create("niws://test/http://localhost:" + port + path);
  }

  private AtomicIntegerArray copy(AtomicIntegerArray src) {
    AtomicIntegerArray dst = new AtomicIntegerArray(src.length());
    for (int i = 0; i < src.length(); ++i) {
      dst.set(i, src.get(i));
    }
    return dst;
  }

  private void assertEquals(AtomicIntegerArray expected, AtomicIntegerArray actual) {
    for (int i = 0; i < expected.length(); ++i) {
      final String prefix = "count(" + i + ")=";
      Assert.assertEquals(prefix + expected.get(i), prefix + actual.get(i));
    }
  }

  private void codeTest(int code, int attempts) throws Exception {
    statusCode.set(code);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(code, attempts);

    rxHttp.get(uri("/empty")).toBlocking().toFuture().get();

    assertEquals(expected, statusCounts);
  }

  @Test
  public void ok() throws Exception {
    codeTest(200, 1);
  }

  @Test
  public void clientError() throws Exception {
    codeTest(400, 1);
  }

  @Test
  public void notFound() throws Exception {
    codeTest(404, 1);
  }

  @Test
  public void serverError() throws Exception {
    codeTest(500, retries + 1);
  }

  @Test
  public void server502() throws Exception {
    codeTest(502, retries + 1);
  }

  @Test
  public void throttle429() throws Exception {
    codeTest(429, retries + 1);
  }

  @Test
  public void throttle503() throws Exception {
    codeTest(503, retries + 1);
  }

  @Test
  public void throttle503NoDelay() throws Exception {
    set(client + ".niws.client.RetryDelay", "0");
    codeTest(503, retries + 1);
  }

  @Test
  public void relativeRedirect() throws Exception {
    set(client + ".niws.client.FollowRedirects", "3");
    int code = 200;
    statusCode.set(code);
    redirects.set(2);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(302, 2);
    expected.addAndGet(code, 1);

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> throwable = new AtomicReference<>();
    rxHttp.get(uri("/relativeRedirect")).subscribe(
        Actions.empty(),
        t -> {
          throwable.set(t);
          latch.countDown();
        },
        latch::countDown
    );

    latch.await();
    Assert.assertNull(throwable.get());
    assertEquals(expected, statusCounts);
  }

  @Test
  public void relativeRedirectAndRetries() throws Exception {
    set(client + ".niws.client.MaxAutoRetriesNextServer", "0");
    relativeRedirect();
  }

  @Test
  public void disablingRedirects() throws Exception {
    set(client + ".niws.client.FollowRedirects", "0");
    set(client + ".niws.client.MaxAutoRetriesNextServer", "1");
    int code = 302;
    statusCode.set(code);
    redirects.set(2);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(code, 1);

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> throwable = new AtomicReference<>();
    rxHttp.get(uri("/relativeRedirect")).subscribe(
        Actions.empty(),
        t -> {
          throwable.set(t);
          t.printStackTrace();
          latch.countDown();
        },
        latch::countDown
    );

    latch.await();
    Assert.assertNull(throwable.get());
    assertEquals(expected, statusCounts);
  }

  @Test
  public void absoluteRedirect() throws Exception {
    set(client + ".niws.client.FollowRedirects", "3");
    int code = 200;
    statusCode.set(code);
    redirects.set(2);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.incrementAndGet(code);
    expected.addAndGet(302, 3);

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> throwable = new AtomicReference<>();
    rxHttp.get(uri("/absoluteRedirect")).subscribe(
        Actions.empty(),
        t -> {
          throwable.set(t);
          latch.countDown();
        },
        latch::countDown
    );

    latch.await();
    Assert.assertNull(throwable.get());
    assertEquals(expected, statusCounts);
  }

  @Test
  public void absoluteRedirectAndRetries() throws Exception {
    set(client + ".niws.client.MaxAutoRetriesNextServer", "0");
    absoluteRedirect();
  }

  @Test
  public void notModified() throws Exception {
    int code = 304;
    statusCode.set(code);
    redirects.set(2);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(code, 1);

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> throwable = new AtomicReference<>();
    rxHttp.get(uri("/notModified")).subscribe(
        Actions.empty(),
        t -> {
          throwable.set(t);
          latch.countDown();
        },
        latch::countDown
    );

    latch.await();
    assertEquals(expected, statusCounts);
    Assert.assertNull(throwable.get());
  }

  @Test
  public void redirectResponseMissingLocation() throws Exception {
    int code = 302;
    statusCode.set(code);
    redirects.set(2);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(code, 1);

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> throwable = new AtomicReference<>();
    rxHttp.get(uri("/redirectNoLocation")).subscribe(
        Actions.empty(),
        t -> {
          throwable.set(t);
          latch.countDown();
        },
        latch::countDown
    );

    latch.await();
    assertEquals(expected, statusCounts);
    Assert.assertNotNull(throwable.get());
  }

  @Test
  public void readTimeout() throws Exception {
    //set(client + ".niws.client.ReadTimeout", "100");
    int code = 200;
    statusCode.set(code);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(code, 3);

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> throwable = new AtomicReference<>();
    rxHttp.get(uri("/readTimeout")).subscribe(
        Actions.empty(),
        t -> {
          throwable.set(t);
          latch.countDown();
        },
        latch::countDown
    );

    latch.await();
    Assert.assertTrue(throwable.get() instanceof ReadTimeoutException);
    assertEquals(expected, statusCounts);
  }

  @Test
  public void readTimeoutDoesntRetry() throws Exception {
    //set(client + ".niws.client.ReadTimeout", "100");
    set(client + ".niws.client.RetryReadTimeouts", "false");
    int code = 200;
    statusCode.set(code);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(code, 1);

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> throwable = new AtomicReference<>();
    rxHttp.get(uri("/readTimeout")).subscribe(
        Actions.empty(),
        t -> {
          throwable.set(t);
          latch.countDown();
        },
        latch::countDown
    );

    latch.await();
    Assert.assertTrue(throwable.get() instanceof ReadTimeoutException);
    assertEquals(expected, statusCounts);
  }

  @Test
  public void connectTimeout() throws Exception {
    // Pick a free port with no server running
    ServerSocket ss = new ServerSocket(0);
    int serverPort = ss.getLocalPort();
    ss.close();

    set(client + ".niws.client.ConnectTimeout", "100");
    int code = 200;
    statusCode.set(code);

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> throwable = new AtomicReference<>();
    rxHttp.get("niws://test/http://localhost:" + serverPort + "/empty").subscribe(
        Actions.empty(),
        t -> {
          throwable.set(t);
          latch.countDown();
        },
        latch::countDown
    );

    latch.await();
    Assert.assertTrue(throwable.get() instanceof ConnectException);
  }

  @Test
  public void simplePost() throws Exception {
    int code = 200;
    statusCode.set(code);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(code, 1);

    final StringBuilder builder = new StringBuilder();
    rxHttp.post(uri("/echo"), "text/plain", "foo bar".getBytes())
        .flatMap(res -> {
          Assert.assertEquals(200, res.getStatus().code());
          return res.getContent();
        })
        .toBlocking()
        .forEach(byteBuf -> builder.append(byteBuf.toString(Charset.defaultCharset())));

    Assert.assertEquals("foo bar", builder.toString());

    assertEquals(expected, statusCounts);
  }

  @Test
  public void gzipPost() throws Exception {
    //set(client + ".niws.client.ReadTimeout", "1000");
    int code = 200;
    statusCode.set(code);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(code, 1);

    StringBuilder content = new StringBuilder();
    for (int i = 0; i < 500; ++i) {
      content.append(i).append(", ");
    }
    String body = content.toString();

    final StringBuilder builder = new StringBuilder();
    rxHttp.post(uri("/echo"), "text/plain", body.getBytes())
        .flatMap(res -> {
          Assert.assertEquals(200, res.getStatus().code());
          return res.getContent();
        })
        .toBlocking()
        .forEach(byteBuf -> builder.append(byteBuf.toString(Charset.defaultCharset())));

    Assert.assertEquals(body, builder.toString());

    assertEquals(expected, statusCounts);
  }

  @Test
  public void postJsonString() throws Exception {
    int code = 200;
    statusCode.set(code);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(code, 1);
    rxHttp.postJson(uri("/empty"), "{}").toBlocking().toFuture().get();
    assertEquals(expected, statusCounts);
  }

  @Test
  public void postForm() throws Exception {
    int code = 200;
    statusCode.set(code);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(code, 1);

    final StringBuilder builder = new StringBuilder();
    rxHttp.postForm(uri("/echo?foo=bar&name=John+Doe&pct=%2042%25"))
        .flatMap(res -> {
          Assert.assertEquals(200, res.getStatus().code());
          return res.getContent();
        })
        .toBlocking()
        .forEach(byteBuf -> builder.append(byteBuf.toString(Charset.defaultCharset())));

    assertEquals(expected, statusCounts);
    Assert.assertEquals("foo=bar&name=John+Doe&pct=%2042%25", builder.toString());
  }

  @Test
  public void putJsonString() throws Exception {
    int code = 200;
    statusCode.set(code);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(code, 1);
    rxHttp.putJson(uri("/empty"), "{}").toBlocking().toFuture().get();
    assertEquals(expected, statusCounts);
  }

  @Test
  public void delete() throws Exception {
    int code = 200;
    statusCode.set(code);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(code, 1);
    rxHttp.delete(uri("/empty").toString()).toBlocking().toFuture().get();
    assertEquals(expected, statusCounts);
  }

  @Test
  public void head() throws Exception {
    int code = 200;
    statusCode.set(code);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(code, 1);
    rxHttp.submit(HttpClientRequest.<ByteBuf>create(HttpMethod.HEAD, uri("/empty").toString()))
        .toBlocking().toFuture().get();
    assertEquals(expected, statusCounts);
  }

  @Test
  public void postWithCustomHeader() throws Exception {
    int code = 200;
    statusCode.set(code);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(code, 1);
    rxHttp.submit(HttpClientRequest.createPost(uri("/empty").toString()).withHeader("k", "v"), "{}")
        .toBlocking().toFuture().get();
    assertEquals(expected, statusCounts);
  }

  @Test
  public void portOverrideSetting() throws Exception {
    set("port-override.niws.client.Port", "2");
    URI origUri = URI.create("niws://port-override/foo");
    URI relUri = URI.create("/foo");
    ClientConfig cfg = new ClientConfig(archaius, "port-override", "vip", origUri, relUri, null);
    InstanceInfo info = InstanceInfo.Builder.newBuilder()
        .setAppName("foo")
        .setPort(1)
        .build();
    Server server = EurekaServerRegistry.toServer(cfg, info);
    Assert.assertEquals(server.port(), 2);
  }

  @Test
  public void portDefaultSetting() throws Exception {
    URI origUri = URI.create("niws://port-default/foo");
    URI relUri = URI.create("/foo");
    ClientConfig cfg = new ClientConfig(archaius, "port-default", "vip", origUri, relUri, null);
    InstanceInfo info = InstanceInfo.Builder.newBuilder()
        .setAppName("foo")
        .setPort(1)
        .build();
    Server server = EurekaServerRegistry.toServer(cfg, info);
    Assert.assertEquals(server.port(), 1);
  }

  private int getPortForReq() throws Exception {
    HttpClientResponse<ByteBuf> r = rxHttp.get(uri("/empty")).toBlocking().toFuture().get();
    return Integer.parseInt(r.getHeaders().get("X-Test-Port"));
  }

  @Test
  public void connectionReuse() throws Exception {
    Map<Integer, Integer> counts = new HashMap<>();
    for (int i = 0; i < 10; ++i) {
      int p = getPortForReq();
      Integer c = counts.get(p);
      if (c == null) {
        counts.put(p, 1);
      } else {
        counts.put(p, c + 1);
      }
    }
    Assert.assertTrue("connections not getting reused", counts.size() < 4);
  }
}

