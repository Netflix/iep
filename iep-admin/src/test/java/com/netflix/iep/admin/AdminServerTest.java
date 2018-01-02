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
package com.netflix.iep.admin;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;


@RunWith(JUnit4.class)
public class AdminServerTest {
  static {
    // For some reason by default CORS headers are dropped
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
  }

  private int port;
  private AdminServer server;

  @Before
  public void before() throws IOException {
    port = getUnusedPort();
    AdminConfig config = new AdminConfig() {
      @Override public int port() {
        return port;
      }

      @Override public int backlog() {
        return 10;
      }

      @Override public Duration shutdownDelay() {
        return Duration.ZERO;
      }

      @Override public String uiLocation() {
        return "/ui";
      }
    };
    Map<String, Object> endpoints = new HashMap<>();
    endpoints.put("/bad",  new BadEndpoint());
    endpoints.put("/test", new TestEndpoint());
    server = new AdminServer(config, endpoints);
  }

  @After
  public void after() throws Exception {
    server.close();
  }

  private int getUnusedPort() throws IOException {
    ServerSocket ss = new ServerSocket(0);
    int p = ss.getLocalPort();
    ss.close();
    return p;
  }

  private Response http(String method, String path, Map<String, String> headers) throws Exception {
    URL url = URI.create("http://localhost:" + port + path).toURL();
    HttpURLConnection con = (HttpURLConnection) url.openConnection();
    con.setReadTimeout(5000);
    con.setConnectTimeout(1000);
    con.setRequestMethod(method);
    con.setInstanceFollowRedirects(false);
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      con.addRequestProperty(entry.getKey(), entry.getValue());
    }
    con.connect();

    int status = con.getResponseCode();
    Map<String, List<String>> resHeaders = con.getHeaderFields();
    String content = null;
    try (InputStream in = (status >= 400) ? con.getErrorStream() : con.getInputStream()) {
      if (in != null) {
        InputStream dataIn = in;
        if (resHeaders.containsKey("Content-encoding")) {
          dataIn = new GZIPInputStream(in);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = dataIn.read(buffer)) > 0) {
          baos.write(buffer, 0, len);
        }
        content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
      }
    }

    return new Response(status, resHeaders, content);
  }

  private Response httpPost(String path, Map<String, String> headers) throws Exception {
    return http("POST", path, headers);
  }

  private Response httpGet(String path, Map<String, String> headers) throws Exception {
    return http("GET", path, headers);
  }

  private Response httpGet(String path) throws Exception {
    return httpGet(path, Collections.emptyMap());
  }

  private Response httpOptions(String path, Map<String, String> headers) throws Exception {
    return http("OPTIONS", path, headers);
  }

  @Test
  public void get() throws Exception {
    Response res = httpGet("/test");
    Assert.assertEquals(200, res.status);
    Assert.assertEquals("\"no-path-set\"", res.content);
  }

  @Test
  public void getGzip() throws Exception {
    Response res = httpGet("/test", Collections.singletonMap("Accept-Encoding", "gzip"));
    Assert.assertEquals(200, res.status);
    Assert.assertEquals("\"no-path-set\"", res.content);
  }

  @Test
  public void getWithPath() throws Exception {
    Response res = httpGet("/test/foo");
    Assert.assertEquals(200, res.status);
    Assert.assertEquals("\"foo\"", res.content);
  }

  @Test
  public void bad() throws Exception {
    Response res = httpGet("/bad");
    Assert.assertEquals(404, res.status);
    Assert.assertEquals("{\"status\":404,\"message\":\"/bad\"}", res.content);
  }

  @Test
  public void badNotFound() throws Exception {
    Response res = httpGet("/bad/not-found");
    Assert.assertEquals(404, res.status);
    Assert.assertEquals("{\"status\":404,\"message\":\"/bad/not-found\"}", res.content);
  }

  @Test
  public void badIllegalArg() throws Exception {
    Response res = httpGet("/bad/arg");
    Assert.assertEquals(400, res.status);
    Assert.assertEquals("{\"status\":400,\"message\":\"IllegalArgumentException: bad\"}", res.content);
  }

  @Test
  public void badIllegalState() throws Exception {
    Response res = httpGet("/bad/state");
    Assert.assertEquals(400, res.status);
    Assert.assertEquals("{\"status\":400,\"message\":\"IllegalStateException: bad\"}", res.content);
  }

  @Test
  public void badInternalServer() throws Exception {
    Response res = httpGet("/bad/server");
    Assert.assertEquals(500, res.status);
    Assert.assertEquals("{\"status\":500,\"message\":\"RuntimeException: bad\"}", res.content);
  }

  @Test
  public void notFoundEndpoint() throws Exception {
    Response res = httpGet("/not-found");
    Assert.assertEquals(404, res.status);
  }

  @Test
  public void unsupportedMethod() throws Exception {
    Response res = httpPost("/test", Collections.emptyMap());
    Assert.assertEquals(405, res.status);
  }

  @Test
  public void uiRedirect() throws Exception {
    Response res = httpGet("/");
    Assert.assertEquals(302, res.status);
    Assert.assertEquals(Collections.singletonList("/ui"), res.headers.get("Location"));

    Assert.assertEquals(302, httpGet("/admin").status);
    Assert.assertEquals(302, httpGet("/baseserver").status);
  }

  @Test
  public void uiPage() throws Exception {
    Response res = httpGet("/ui");
    Assert.assertEquals(200, res.status);
    Assert.assertEquals(Collections.singletonList("text/html"), res.headers.get("Content-type"));
    Assert.assertTrue(res.content.contains("<title>Test UI</title>"));
  }

  @Test
  public void staticContent() throws Exception {
    Response res = httpGet("/static/test.txt");
    Assert.assertEquals(200, res.status);
    Assert.assertEquals(Collections.singletonList("text/plain"), res.headers.get("Content-type"));
    Assert.assertEquals("test of static content", res.content);
  }

  @Test
  public void cors() throws Exception {
    // Pre-flight
    Response res = httpOptions("/test", Collections.singletonMap("Origin", "foo"));

    Assert.assertEquals(
        Collections.singletonList("foo"),
        res.headers.get("Access-control-allow-origin"));

    Assert.assertEquals(
        Collections.singletonList("GET, HEAD"),
        res.headers.get("Access-control-allow-methods"));

    // Get
    res = httpGet("/test", Collections.singletonMap("Origin", "foo"));
    Assert.assertEquals(200, res.status);
    Assert.assertEquals("\"no-path-set\"", res.content);

    Assert.assertEquals(
        Collections.singletonList("foo"),
        res.headers.get("Access-control-allow-origin"));

    Assert.assertEquals(
        Collections.singletonList("GET, HEAD"),
        res.headers.get("Access-control-allow-methods"));
  }

  @Test
  public void resources() throws Exception {
    Response res = httpGet("/resources");
    Assert.assertEquals(200, res.status);
    Assert.assertEquals("[\"bad\",\"resources\",\"test\"]", res.content);
    Assert.assertEquals(404, httpGet("/resources/test").status);
  }

  static class Response {
    final int status;
    final Map<String, List<String>> headers;
    final String content;

    Response(int status, Map<String, List<String>> headers, String content) {
      this.status = status;
      this.headers = headers;
      this.content = content;
    }

    Response() {
      this(-1, Collections.emptyMap(), null);
    }
  }

  public static class TestEndpoint {
    public Object get() {
      return "no-path-set";
    }

    public Object get(String path) {
      return path;
    }
  }

  public static class BadEndpoint {
    public Object get() {
      return null;
    }

    public Object get(String path) {
      switch (path) {
        case "arg":       throw new IllegalArgumentException("bad");
        case "state":     throw new IllegalStateException("bad");
        case "server":    throw new RuntimeException("bad");
        case "not-found": return null;
        default:          return path;
      }
    }
  }
}
