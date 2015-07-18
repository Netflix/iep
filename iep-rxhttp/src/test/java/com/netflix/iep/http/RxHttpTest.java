/*
 * Copyright 2015 Netflix, Inc.
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
import com.netflix.config.ConfigurationManager;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.timeout.ReadTimeoutException;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import org.apache.commons.configuration.Configuration;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.functions.Func1;

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
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(JUnit4.class)
public class RxHttpTest {

  private static final String client = "test";
  private static final int retries = 2;

  private static HttpServer server;
  private static int port;

  private static AtomicInteger statusCode = new AtomicInteger(200);
  private static AtomicIntegerArray statusCounts = new AtomicIntegerArray(600);

  private static AtomicInteger redirects = new AtomicInteger(0);

  private static final Configuration archaius = ConfigurationManager.getConfigInstance();
  private static final RxHttp rxHttp = new RxHttp(archaius, null);

  private static void set(String k, String v) {
    ConfigurationManager.getConfigInstance().setProperty(k, v);
  }

  private static void ignore(InputStream input) throws IOException {
    try (InputStream in = input) {
      byte[] buf = new byte[1024];
      while (in.read(buf) > 0);
    }
  }

  @BeforeClass
  public static void startServer() throws Exception {
    rxHttp.start();

    server = HttpServer.create(new InetSocketAddress(0), 100);
    server.setExecutor(Executors.newFixedThreadPool(10, new ThreadFactory() {
      @Override public Thread newThread(Runnable r) {
        return new Thread(r, "HttpServer");
      }
    }));
    port = server.getAddress().getPort();

    server.createContext("/empty", new HttpHandler() {
      @Override
      public void handle(HttpExchange exchange) throws IOException {
System.out.println("/empty: start " + statusCode.get());
        ignore(exchange.getRequestBody());
        int port = exchange.getRemoteAddress().getPort();
        exchange.getResponseHeaders().add("X-Test-Port", "" + port);
        statusCounts.incrementAndGet(statusCode.get());
        exchange.sendResponseHeaders(statusCode.get(), -1L);
        exchange.close();
System.out.println("/empty: end " + statusCode.get());
      }
    });

    server.createContext("/echo", new HttpHandler() {
      @Override
      public void handle(HttpExchange exchange) throws IOException {
        Headers headers = exchange.getRequestHeaders();
        int contentLength = Integer.parseInt(headers.getFirst("Content-Length"));
        String contentEnc = headers.getFirst("Content-Encoding");
        if (contentEnc != null) {
          exchange.getResponseHeaders().add("Content-Encoding", contentEnc);
        }

        int code = statusCode.get();
        if (contentLength > 512 && !"gzip".equals(contentEnc)) {
System.out.println(contentEnc);
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
      }
    });

    server.createContext("/relativeRedirect", new HttpHandler() {
      @Override
      public void handle(HttpExchange exchange) throws IOException {
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
      }
    });

    server.createContext("/absoluteRedirect", new HttpHandler() {
      @Override
      public void handle(HttpExchange exchange) throws IOException {
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
      }
    });

    server.createContext("/notModified", new HttpHandler() {
      @Override
      public void handle(HttpExchange exchange) throws IOException {
        ignore(exchange.getRequestBody());
        statusCounts.incrementAndGet(304);
        exchange.sendResponseHeaders(304, -1L);
        exchange.close();
      }
    });

    server.createContext("/redirectNoLocation", new HttpHandler() {
      @Override
      public void handle(HttpExchange exchange) throws IOException {
        ignore(exchange.getRequestBody());
        statusCounts.incrementAndGet(302);
        exchange.sendResponseHeaders(302, -1L);
        exchange.close();
      }
    });

    server.createContext("/readTimeout", new HttpHandler() {
      @Override
      public void handle(HttpExchange exchange) throws IOException {
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
      }
    });

    server.start();

    set(client + ".niws.client.MaxAutoRetriesNextServer", "" + retries);
    set(client + ".niws.client.RetryDelay", "100");
    set(client + ".niws.client.ReadTimeout", "1000");
  }

  @AfterClass
  public static void stopServer() {
    rxHttp.stop();
    server.stop(0);
  }

  @Before
  public void initProps() {
    set(client + ".niws.client.MaxAutoRetriesNextServer", "" + retries);
    set(client + ".niws.client.RetryDelay", "100");
    set(client + ".niws.client.ConnectTimeout", "1000");
    set(client + ".niws.client.ReadTimeout", "30000");
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

  private long delta(long t) {
    return System.currentTimeMillis() - t;
  }

  private void codeTest(final int code, final int attempts) throws Exception {
System.out.println("STARTING codeTest: " + code + ", " + attempts);
    statusCode.set(code);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(code, attempts);

    final long now = System.currentTimeMillis();

    rxHttp.get(uri("/empty"))
    .doOnNext(new Action1<HttpClientResponse<ByteBuf>>() {
      @Override
      public void call(HttpClientResponse<ByteBuf> res) {
System.out.println("next [" + delta(now) + "]: codeTest(" + code + ", " + attempts + ")");
        res.discardContent();
      }
    })
.doOnError(new Action1<Throwable>() {
  @Override
  public void call(Throwable t) {
    System.out.println("error [" + delta(now) + "]: codeTest(" + code + ", " + attempts + ") -> " + t);
  }
})
.doOnCompleted(new Action0() {
  @Override
  public void call() {
    System.out.println("completed [" + delta(now) + "]: codeTest(" + code + ", " + attempts + ")");
  }
})
.doOnTerminate(new Action0() {
  @Override
  public void call() {
    System.out.println("terminate [" + delta(now) + "]: codeTest(" + code + ", " + attempts + ")");
  }
})
.doOnSubscribe(new Action0() {
  @Override
  public void call() {
    System.out.println("subscribe [" + delta(now) + "]: codeTest(" + code + ", " + attempts + ")");
  }
})
.doOnUnsubscribe(new Action0() {
  @Override
  public void call() {
    System.out.println("unsubscribe [" + delta(now) + "]: codeTest(" + code + ", " + attempts + ")");
  }
})
    .timeout(9, TimeUnit.SECONDS)
    .toBlocking().toFuture().get(10, TimeUnit.SECONDS);

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
    int code = 200;
    statusCode.set(code);
    redirects.set(2);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(302, 2);
    expected.addAndGet(code, 1);

    final AtomicReference<Throwable> throwable = new AtomicReference<>();
    try {
      rxHttp.get(uri("/relativeRedirect"))
      .doOnNext(new Action1<HttpClientResponse<ByteBuf>>() {
        @Override
        public void call(HttpClientResponse<ByteBuf> res) {
          res.discardContent();
        }
      })
      .doOnError(new Action1<Throwable>() {
        @Override public void call(Throwable t) {
          throwable.set(t);
        }
      })
      .toBlocking().toFuture().get(10, TimeUnit.SECONDS);
    }
    catch (Throwable t) {}

    assertEquals(expected, statusCounts);
    Assert.assertNull(throwable.get());
  }

  @Test
  public void absoluteRedirect() throws Exception {
    int code = 200;
    statusCode.set(code);
    redirects.set(2);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.incrementAndGet(code);
    expected.addAndGet(302, 3);

    final AtomicReference<Throwable> throwable = new AtomicReference<>();
    try {
      rxHttp.get(uri("/absoluteRedirect"))
      .doOnNext(new Action1<HttpClientResponse<ByteBuf>>() {
        @Override
        public void call(HttpClientResponse<ByteBuf> res) {
          res.discardContent();
        }
      })
      .doOnError(new Action1<Throwable>() {
        @Override public void call(Throwable t) {
          throwable.set(t);
        }
      })
      .toBlocking().toFuture().get(10, TimeUnit.SECONDS);
    }
    catch (Throwable t) {}

    assertEquals(expected, statusCounts);
    Assert.assertNull(throwable.get());
  }

  @Test
  public void notModified() throws Exception {
    int code = 304;
    statusCode.set(code);
    redirects.set(2);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(code, 1);

    final AtomicReference<Throwable> throwable = new AtomicReference<>();
    try {
      rxHttp.get(uri("/notModified"))
      .doOnNext(new Action1<HttpClientResponse<ByteBuf>>() {
        @Override
        public void call(HttpClientResponse<ByteBuf> res) {
          res.discardContent();
        }
      })
      .doOnError(new Action1<Throwable>() {
        @Override public void call(Throwable t) {
          throwable.set(t);
        }
      })
      .toBlocking().toFuture().get(10, TimeUnit.SECONDS);
    }
    catch (Throwable t) {}

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

    final AtomicReference<Throwable> throwable = new AtomicReference<>();
    try {
      rxHttp.get(uri("/redirectNoLocation"))
      .doOnNext(new Action1<HttpClientResponse<ByteBuf>>() {
        @Override
        public void call(HttpClientResponse<ByteBuf> res) {
          res.discardContent();
        }
      })
      .doOnError(new Action1<Throwable>() {
        @Override public void call(Throwable t) {
          throwable.set(t);
        }
      })
      .toBlocking().toFuture().get(10, TimeUnit.SECONDS);
    }
    catch (Throwable t) {}

    assertEquals(expected, statusCounts);
    Assert.assertNotNull(throwable.get());
  }

  @Test
  public void readTimeout() throws Exception {
    set(client + ".niws.client.ReadTimeout", "100");
    int code = 200;
    statusCode.set(code);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(code, 3);

    final AtomicReference<Throwable> throwable = new AtomicReference<>();
    try {
      rxHttp.get(uri("/readTimeout"))
      .doOnNext(new Action1<HttpClientResponse<ByteBuf>>() {
        @Override
        public void call(HttpClientResponse<ByteBuf> res) {
          res.discardContent();
        }
      })
      .doOnError(new Action1<Throwable>() {
        @Override public void call(Throwable t) {
          throwable.set(t);
        }
      })
      .toBlocking().toFuture().get(10, TimeUnit.SECONDS);
    }
    catch (Throwable t) {}

    Assert.assertTrue(throwable.get() instanceof ReadTimeoutException);
    assertEquals(expected, statusCounts);
  }

  @Test
  public void readTimeoutDoesntRetry() throws Exception {
    set(client + ".niws.client.ReadTimeout", "100");
    set(client + ".niws.client.RetryReadTimeouts", "false");
    int code = 200;
    statusCode.set(code);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(code, 1);

    final AtomicReference<Throwable> throwable = new AtomicReference<>();
    try {
      rxHttp.get(uri("/readTimeout"))
      .doOnNext(new Action1<HttpClientResponse<ByteBuf>>() {
        @Override
        public void call(HttpClientResponse<ByteBuf> res) {
          res.discardContent();
        }
      })
      .doOnError(new Action1<Throwable>() {
        @Override public void call(Throwable t) {
          throwable.set(t);
        }
      })
      .toBlocking().toFuture().get(10, TimeUnit.SECONDS);
    }
    catch (Throwable t) {}

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

    final AtomicReference<Throwable> throwable = new AtomicReference<>();
    try {
      rxHttp.get("niws://test/http://localhost:" + serverPort + "/empty")
      .doOnNext(new Action1<HttpClientResponse<ByteBuf>>() {
        @Override
        public void call(HttpClientResponse<ByteBuf> res) {
          res.discardContent();
        }
      })
      .doOnError(new Action1<Throwable>() {
        @Override
        public void call(Throwable t) {
          throwable.set(t);
        }
      })
      .toBlocking().toFuture().get(10, TimeUnit.SECONDS);
    }
    catch (Throwable t) {}

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
        .flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<ByteBuf>>() {
          @Override
          public Observable<ByteBuf> call(HttpClientResponse<ByteBuf> res) {
            Assert.assertEquals(200, res.getStatus().code());
            return res.getContent();
          }
        })
        .doOnNext(new Action1<ByteBuf>() {
          @Override
          public void call(ByteBuf byteBuf) {
            builder.append(byteBuf.toString(Charset.defaultCharset()));
byteBuf.release();
          }
        })
        .toBlocking().toFuture().get(10, TimeUnit.SECONDS);

    Assert.assertEquals("foo bar", builder.toString());

    assertEquals(expected, statusCounts);
  }

/**
  TODO
  @Test
  public void gzipPost() throws Exception {
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
        .flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<ByteBuf>>() {
          @Override
          public Observable<ByteBuf> call(HttpClientResponse<ByteBuf> res) {
            Assert.assertEquals(200, res.getStatus().code());
            return res.getContent();
          }
        })
        .forEach(new Action1<ByteBuf>() {
          @Override
          public void call(ByteBuf byteBuf) {
            builder.append(byteBuf.toString(Charset.defaultCharset()));
byteBuf.release();
          }
        })
        .toBlocking().toFuture().get(10, TimeUnit.SECONDS);

    Assert.assertEquals(body, builder.toString());

    assertEquals(expected, statusCounts);
  }
*/

  @Test
  public void postJsonString() throws Exception {
    int code = 200;
    statusCode.set(code);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(code, 1);
    rxHttp.postJson(uri("/empty"), "{}")
    .doOnNext(new Action1<HttpClientResponse<ByteBuf>>() {
      @Override
      public void call(HttpClientResponse<ByteBuf> res) {
        res.discardContent();
      }
    })
    .toBlocking().toFuture().get(10, TimeUnit.SECONDS);
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
        .flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<ByteBuf>>() {
          @Override
          public Observable<ByteBuf> call(HttpClientResponse<ByteBuf> res) {
            Assert.assertEquals(200, res.getStatus().code());
            return res.getContent();
          }
        })
        .doOnNext(new Action1<ByteBuf>() {
          @Override
          public void call(ByteBuf byteBuf) {
            builder.append(byteBuf.toString(Charset.defaultCharset()));
byteBuf.release();
          }
        })
        .toBlocking().toFuture().get(10, TimeUnit.SECONDS);

    assertEquals(expected, statusCounts);
    Assert.assertEquals("foo=bar&name=John+Doe&pct=%2042%25", builder.toString());
  }

  @Test
  public void putJsonString() throws Exception {
    int code = 200;
    statusCode.set(code);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(code, 1);
    rxHttp.putJson(uri("/empty"), "{}")
    .doOnNext(new Action1<HttpClientResponse<ByteBuf>>() {
      @Override
      public void call(HttpClientResponse<ByteBuf> res) {
        res.discardContent();
      }
    })
    .toBlocking().toFuture().get(10, TimeUnit.SECONDS);
    assertEquals(expected, statusCounts);
  }

  @Test
  public void delete() throws Exception {
    int code = 200;
    statusCode.set(code);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(code, 1);
    rxHttp.delete(uri("/empty").toString())
    .doOnNext(new Action1<HttpClientResponse<ByteBuf>>() {
      @Override
      public void call(HttpClientResponse<ByteBuf> res) {
        res.discardContent();
      }
    })
    .toBlocking().toFuture().get(10, TimeUnit.SECONDS);
    assertEquals(expected, statusCounts);
  }

  @Test
  public void head() throws Exception {
    int code = 200;
    statusCode.set(code);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(code, 1);
    rxHttp.submit(HttpRequest.create(HttpMethod.HEAD, uri("/empty").toString()))
    .doOnNext(new Action1<HttpClientResponse<ByteBuf>>() {
      @Override
      public void call(HttpClientResponse<ByteBuf> res) {
        res.discardContent();
      }
    })
    .toBlocking().toFuture().get(10, TimeUnit.SECONDS);
    assertEquals(expected, statusCounts);
  }

  @Test
  public void postWithCustomHeader() throws Exception {
    int code = 200;
    statusCode.set(code);
    AtomicIntegerArray expected = copy(statusCounts);
    expected.addAndGet(code, 1);
    rxHttp.submit(HttpRequest.createPost(uri("/empty").toString()).addHeader("k", "v"), "{}")
    .doOnNext(new Action1<HttpClientResponse<ByteBuf>>() {
      @Override
      public void call(HttpClientResponse<ByteBuf> res) {
        res.discardContent();
      }
    })
    .toBlocking().toFuture().get(10, TimeUnit.SECONDS);
    assertEquals(expected, statusCounts);
  }

  @Test
  public void portOverrideSetting() throws Exception {
    set("port-override.niws.client.Port", "2");
    URI origUri = URI.create("niws://port-override/foo");
    URI relUri = URI.create("/foo");
    ClientConfig cfg = new ClientConfig(archaius, "port-override", "vip", origUri, relUri);
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
    ClientConfig cfg = new ClientConfig(archaius, "port-default", "vip", origUri, relUri);
    InstanceInfo info = InstanceInfo.Builder.newBuilder()
        .setAppName("foo")
        .setPort(1)
        .build();
    Server server = EurekaServerRegistry.toServer(cfg, info);
    Assert.assertEquals(server.port(), 1);
  }

  private int getPortForReq() throws Exception {
    HttpClientResponse<ByteBuf> r = rxHttp.get(uri("/empty"))
    .doOnNext(new Action1<HttpClientResponse<ByteBuf>>() {
      @Override
      public void call(HttpClientResponse<ByteBuf> res) {
        res.discardContent();
      }
    })
    .toBlocking().toFuture().get(10, TimeUnit.SECONDS);
    return Integer.parseInt(r.getHeader("X-Test-Port"));
  }

/**
  TODO
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
*/
}

