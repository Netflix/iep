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

import com.netflix.config.ConfigurationManager;
import com.netflix.spectator.api.Spectator;
import com.netflix.spectator.impl.Preconditions;
import com.netflix.spectator.sandbox.HttpLogEntry;
//import io.reactivex.netty.client.CompositePoolLimitDeterminationStrategy;
//import io.reactivex.netty.client.MaxConnectionsBasedStrategy;
//import io.reactivex.netty.client.PoolLimitDeterminationStrategy;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.logging.LogLevel;
import org.apache.commons.configuration.Configuration;
import rx.functions.Actions;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.timeout.ReadTimeoutException;
//import io.reactivex.netty.RxNetty;
//import io.reactivex.netty.pipeline.PipelineConfigurator;
//import io.reactivex.netty.pipeline.PipelineConfiguratorComposite;
//import io.reactivex.netty.pipeline.ssl.DefaultFactories;
import io.reactivex.netty.protocol.http.client.HttpClient;
//import io.reactivex.netty.protocol.http.client.HttpClientBuilder;
//import io.reactivex.netty.protocol.http.client.HttpClientPipelineConfigurator;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import rx.Observable;
import rx.functions.Action1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

/**
 * Helper for some simple uses of rxnetty with eureka. Only intended for use within the spectator
 * plugin.
 */
@Singleton
public final class RxHttp {

  private static final Logger LOGGER = LoggerFactory.getLogger(RxHttp.class);

  private static final String APPLICATION_JSON = "application/json";

  private static final int MIN_COMPRESS_SIZE = 512;
  private static final AtomicInteger NEXT_THREAD_ID = new AtomicInteger(0);

  //private final ConcurrentHashMap<String, PoolLimitDeterminationStrategy> poolLimits = new ConcurrentHashMap<>();

  private final ConcurrentHashMap<Server, HttpClient<ByteBuf, ByteBuf>> clients = new ConcurrentHashMap<>();
  private ScheduledExecutorService executor;

  private final Configuration config;
  private final ServerRegistry serverRegistry;

  /**
   * Create a new instance using the specified server registry. Calls using client side
   * load-balancing (niws:// or vip:// URIs) need a server registry to lookup the set of servers
   * to balance over.
   *
   * @deprecated Use {@link RxHttp#RxHttp(Configuration, ServerRegistry)} instead.
   */
  @Deprecated
  public RxHttp(ServerRegistry serverRegistry) {
    this(ConfigurationManager.getConfigInstance(), serverRegistry);
  }

  /**
   * Create a new instance using the specified server registry. Calls using client side
   * load-balancing (niws:// or vip:// URIs) need a server registry to lookup the set of servers
   * to balance over.
   */
  @Inject
  public RxHttp(Configuration config, ServerRegistry serverRegistry) {
    this.config = config;
    this.serverRegistry = serverRegistry;
  }



  /**
   * Setup the background tasks for cleaning up connections.
   */
  @PostConstruct
  public void start() {
    LOGGER.info("starting up backround cleanup threads");
    executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
      @Override public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "spectator-rxhttp-" + NEXT_THREAD_ID.getAndIncrement());
        t.setDaemon(true);
        return t;
      }
    });

    Runnable task = new Runnable() {
      @Override public void run() {
        try {
          LOGGER.debug("executing cleanup for {} clients", clients.size());
          for (Map.Entry<Server, HttpClient<ByteBuf, ByteBuf>> entry : clients.entrySet()) {
            final Server s = entry.getKey();
            if (s.isRegistered() && !serverRegistry.isStillAvailable(s)) {
              LOGGER.debug("cleaning up client for {}", s);
              clients.remove(s);
              // TODO: how to shutdown clients?
              // entry.getValue().shutdown();
            }
          }
          LOGGER.debug("cleanup complete with {} clients remaining", clients.size());
        } catch (Exception e) {
          LOGGER.warn("connection cleanup task failed", e);
        }
      }
    };

    final long cleanupFreq = Spectator.config().getLong("spectator.http.cleanupFrequency", 60);
    executor.scheduleWithFixedDelay(task, 0L, cleanupFreq, TimeUnit.SECONDS);
  }

  /**
   * Shutdown all connections that are currently open.
   */
  @PreDestroy
  public void stop() {
    LOGGER.info("shutting down background cleanup threads");
    executor.shutdown();
    for (HttpClient<ByteBuf, ByteBuf> client : clients.values()) {
      // TODO: how to shutdown clients?
      // client.shutdown();
    }
  }

  private static HttpRequest compress(
      ClientConfig clientCfg, HttpRequest req, byte[] entity) {
/**
    if (entity.length >= MIN_COMPRESS_SIZE && clientCfg.gzipEnabled()) {
      req.addHeader(HttpHeaders.Names.CONTENT_ENCODING, HttpHeaders.Values.GZIP);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
        gzip.write(entity);
      } catch (IOException e) {
        // This isn't expected to occur
        throw new RuntimeException("failed to gzip request payload", e);
      }
      return req
          .addHeader(HttpHeaders.Names.CONTENT_ENCODING, HttpHeaders.Values.GZIP)
          //.writeStringContent();
          .setContent(baos.toByteArray());
    } else {
      req.withContent(entity);
    }
*/
    return req;
  }

  /** Create a log entry for an rxnetty request. */
  public static HttpLogEntry create(HttpRequest req) {
    HttpLogEntry entry = new HttpLogEntry()
        .withMethod(req.method().name())
        .withRequestUri(req.uri())
        .withRequestContentLength(req.getContentLength());

    for (HttpHeader h : req.headers()) {
      entry.withRequestHeader(h.name().toString(), h.value().toString());
    }

    return entry;
  }

  private static HttpLogEntry create(ClientConfig cfg, HttpRequest req) {
    return create(req)
        .withClientName(cfg.name())
        .withOriginalUri(cfg.originalUri())
        .withMaxAttempts(cfg.numRetries() + 1);
  }

  private static void update(HttpLogEntry entry, HttpClientResponse<ByteBuf> res) {
    int code = res.getStatus().code();
    boolean canRetry = (code == 429 || code >= 500);
    entry.mark("received-response")
        .withStatusCode(code)
        .withStatusReason(res.getStatus().reasonPhrase())
        .withResponseContentLength(res.getContentLength(-1))
        .withCanRetry(canRetry);

    for (String k : res.getHeaderNames()) {
      entry.withResponseHeader(k, res.getHeader(k));
    }
  }

  private void update(HttpLogEntry entry, Throwable t) {
    boolean canRetry = (t instanceof ConnectException || t instanceof ReadTimeoutException);
    entry.mark("received-error").withException(t).withCanRetry(canRetry);
  }

  /**
   * Perform a GET request.
   *
   * @param uri
   *     Location to send the request.
   * @return
   *     Observable with the response of the request.
   */
  public Observable<HttpClientResponse<ByteBuf>> get(String uri) {
    return submit(HttpRequest.createGet(uri));
  }

  /**
   * Perform a GET request.
   *
   * @param uri
   *     Location to send the request.
   * @return
   *     Observable with the response of the request.
   */
  public Observable<HttpClientResponse<ByteBuf>> get(URI uri) {
    return submit(HttpRequest.createGet(uri.toString()));
  }

  /**
   * Perform a GET request expecting a JSON response.
   *
   * @param uri
   *     Location to send the request.
   * @return
   *     Observable with the response of the request.
   */
  public Observable<HttpClientResponse<ByteBuf>> getJson(String uri) {
    return getJson(URI.create(uri));
  }

  /**
   * Perform a GET request expecting a JSON response.
   *
   * @param uri
   *     Location to send the request.
   * @return
   *     Observable with the response of the request.
   */
  public Observable<HttpClientResponse<ByteBuf>> getJson(URI uri) {
    final HttpRequest req = HttpRequest.createGet(uri.toString())
        .addHeader(HttpHeaders.Names.ACCEPT, APPLICATION_JSON);
    return submit(req);
  }

  /**
   * Perform a POST request.
   *
   * @param uri
   *     Location to send the data.
   * @param contentType
   *     MIME type for the request payload.
   * @param entity
   *     Data to send.
   * @return
   *     Observable with the response of the request.
   */
  public Observable<HttpClientResponse<ByteBuf>>
  post(URI uri, String contentType, byte[] entity) {
    final HttpRequest req = HttpRequest.createPost(uri.toString())
        .withContent(contentType, entity);
    return submit(req);
  }

  /**
   * Perform a POST request with {@code Content-Type: application/json}.
   *
   * @param uri
   *     Location to send the data.
   * @param entity
   *     Data to send.
   * @return
   *     Observable with the response of the request.
   */
  public Observable<HttpClientResponse<ByteBuf>> postJson(URI uri, byte[] entity) {
    final HttpRequest req = HttpRequest.createPost(uri.toString())
        .addHeader(HttpHeaders.Names.CONTENT_TYPE, APPLICATION_JSON)
        .addHeader(HttpHeaders.Names.ACCEPT, APPLICATION_JSON);
    return submit(req, entity);
  }

  /**
   * Perform a POST request with {@code Content-Type: application/json}.
   *
   * @param uri
   *     Location to send the data.
   * @param entity
   *     Data to send.
   * @return
   *     Observable with the response of the request.
   */
  public Observable<HttpClientResponse<ByteBuf>> postJson(URI uri, String entity) {
    return postJson(uri, getBytes(entity));
  }

  /**
   * Perform a POST request with form data. The body will be extracted from the query string
   * in the URI.
   *
   * @param uri
   *     Location to send the data.
   * @return
   *     Observable with the response of the request.
   */
  public Observable<HttpClientResponse<ByteBuf>> postForm(URI uri) {
    Preconditions.checkNotNull(uri.getRawQuery(), "uri.query");
    byte[] entity = getBytes(uri.getRawQuery());
    return post(uri, HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED, entity);
  }

  /**
   * Perform a PUT request.
   *
   * @param uri
   *     Location to send the data.
   * @param contentType
   *     MIME type for the request payload.
   * @param entity
   *     Data to send.
   * @return
   *     Observable with the response of the request.
   */
  public Observable<HttpClientResponse<ByteBuf>>
  put(URI uri, String contentType, byte[] entity) {
    final HttpRequest req = HttpRequest.createPut(uri.toString())
        .addHeader(HttpHeaders.Names.CONTENT_TYPE, contentType);
    return submit(req, entity);
  }

  /**
   * Perform a PUT request with {@code Content-Type: application/json}.
   *
   * @param uri
   *     Location to send the data.
   * @param entity
   *     Data to send.
   * @return
   *     Observable with the response of the request.
   */
  public Observable<HttpClientResponse<ByteBuf>> putJson(URI uri, byte[] entity) {
    final HttpRequest req = HttpRequest.createPut(uri.toString())
        .addHeader(HttpHeaders.Names.CONTENT_TYPE, APPLICATION_JSON)
        .addHeader(HttpHeaders.Names.ACCEPT, APPLICATION_JSON);
    return submit(req, entity);
  }

  /**
   * Perform a PUT request with {@code Content-Type: application/json}.
   *
   * @param uri
   *     Location to send the data.
   * @param entity
   *     Data to send.
   * @return
   *     Observable with the response of the request.
   */
  public Observable<HttpClientResponse<ByteBuf>> putJson(URI uri, String entity) {
    return putJson(uri, getBytes(entity));
  }

  /**
   * Perform a DELETE request.
   *
   * @param uri
   *     Location to send the request.
   * @return
   *     Observable with the response of the request.
   */
  public Observable<HttpClientResponse<ByteBuf>> delete(String uri) {
    return submit(HttpRequest.createDelete(uri));
  }

  /**
   * Perform a DELETE request.
   *
   * @param uri
   *     Location to send the request.
   * @return
   *     Observable with the response of the request.
   */
  public Observable<HttpClientResponse<ByteBuf>> delete(URI uri) {
    return submit(HttpRequest.createDelete(uri.toString()));
  }

  /**
   * Perform a DELETE request expecting a JSON response.
   *
   * @param uri
   *     Location to send the request.
   * @return
   *     Observable with the response of the request.
   */
  public Observable<HttpClientResponse<ByteBuf>> deleteJson(String uri) {
    return deleteJson(URI.create(uri));
  }

  /**
   * Perform a DELETE request expecting a JSON response.
   *
   * @param uri
   *     Location to send the request.
   * @return
   *     Observable with the response of the request.
   */
  public Observable<HttpClientResponse<ByteBuf>> deleteJson(URI uri) {
    final HttpRequest req = HttpRequest.createDelete(uri.toString())
        .addHeader(HttpHeaders.Names.ACCEPT, APPLICATION_JSON);
    return submit(req);
  }

  /**
   * Submit an HTTP request.
   *
   * @param req
   *     Request to execute. Note the content should be passed in separately not already passed
   *     to the request. The RxNetty request object doesn't provide a way to get the content
   *     out via the public api, so we need to keep it separate in case a new request object must
   *     be created.
   * @return
   *     Observable with the response of the request.
   */
  public Observable<HttpClientResponse<ByteBuf>> submit(HttpRequest req) {
    return submit(req, (byte[]) null);
  }

  /**
   * Submit an HTTP request.
   *
   * @param req
   *     Request to execute. Note the content should be passed in separately not already passed
   *     to the request. The RxNetty request object doesn't provide a way to get the content
   *     out via the public api, so we need to keep it separate in case a new request object must
   *     be created.
   * @param entity
   *     Content data or null if no content is needed for the request body.
   * @return
   *     Observable with the response of the request.
   */
  public Observable<HttpClientResponse<ByteBuf>>
  submit(HttpRequest req, String entity) {
    return submit(req, (entity == null) ? null : getBytes(entity));
  }

  /**
   * Submit an HTTP request.
   *
   * @param req
   *     Request to execute. Note the content should be passed in separately not already passed
   *     to the request. The RxNetty request object doesn't provide a way to get the content
   *     out via the public api, so we need to keep it separate in case a new request object must
   *     be created.
   * @param entity
   *     Content data or null if no content is needed for the request body.
   * @return
   *     Observable with the response of the request.
   */
  public Observable<HttpClientResponse<ByteBuf>>
  submit(HttpRequest req, byte[] entity) {
    final ClientConfig clientCfg = ClientConfig.fromUri(config, req.uri());
    final List<Server> servers = getServers(clientCfg);
    final String reqUri = clientCfg.relativeUri();
    final HttpRequest newReq = req.withUri(reqUri);
    final HttpRequest finalReq = (entity == null)
        ? newReq
        : compress(clientCfg, newReq, entity);
    return execute(clientCfg, servers, finalReq);
  }

  /**
   * Execute an HTTP request.
   *
   * @param clientCfg
   *     Configuration settings for the request.
   * @param servers
   *     List of servers to attempt. The servers will be tried in order until a successful
   *     response or a non-retriable error occurs. For status codes 429 and 503 the
   *     {@code Retry-After} header is honored. Otherwise back-off will be based on the
   *     {@code RetryDelay} config setting.
   * @param req
   *     Request to execute.
   * @return
   *     Observable with the response of the request.
   */
  Observable<HttpClientResponse<ByteBuf>>
  execute(final ClientConfig clientCfg, final List<Server> servers, final HttpRequest req) {
    final HttpLogEntry entry = create(clientCfg, req);

    if (servers.isEmpty()) {
      final String msg = "empty server list for client " + clientCfg.name();
      return Observable.error(new IllegalStateException(msg));
    }

    if (clientCfg.gzipEnabled()) {
      req.addHeader(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
    }

    final RequestContext context = new RequestContext(this, entry, req, clientCfg, servers.get(0));
    final long backoffMillis = clientCfg.retryDelay();
    Observable<HttpClientResponse<ByteBuf>> observable = execute(context);
    for (int i = 1; i < servers.size(); ++i) {
      final RequestContext ctxt = context.withServer(servers.get(i));
      final long delay = backoffMillis << (i - 1);
      final int attempt = i + 1;
      observable = observable
          .flatMap(new RedirectHandler(ctxt))
          .flatMap(new StatusRetryHandler(ctxt, attempt, delay))
          .onErrorResumeNext(new ErrorRetryHandler(ctxt, attempt));
    }

    return observable;
  }

  /**
   * Execute an HTTP request.
   *
   * @param context
   *     Context associated with the request.
   * @return
   *     Observable with the response of the request.
   */
  Observable<HttpClientResponse<ByteBuf>> execute(final RequestContext context) {
    final HttpLogEntry entry = context.entry();

    final HttpClient<ByteBuf, ByteBuf> client = getClient(context);
    entry.mark("start");
    entry.withRemoteAddr(context.server().host());
    entry.withRemotePort(context.server().port());

    final HttpRequest request = context.request();
    HttpClientRequest<ByteBuf, ByteBuf> clientReq = client.createRequest(request.method(), request.uri().toString());
clientReq.enableWireLogging(LogLevel.ERROR);
    for (HttpHeader h : request.headers()) {
      clientReq = clientReq.addHeader(h.name(), h.value());
    }
    return clientReq.writeContent(request.content())
        .doOnNext(new Action1<HttpClientResponse<ByteBuf>>() {
          @Override
          public void call(HttpClientResponse<ByteBuf> res) {
System.out.println("doOnNext");
            update(entry, res);
            HttpLogEntry.logClientRequest(entry);
          }
        })
        .doOnError(new Action1<Throwable>() {
          @Override public void call(Throwable throwable) {
System.out.println("doOnError - " + throwable);
            update(entry, throwable);
            HttpLogEntry.logClientRequest(entry);
          }
        });
        //.doOnTerminate(Actions.empty());
  }

  private HttpClient<ByteBuf, ByteBuf> getClient(final RequestContext context) {
    HttpClient<ByteBuf, ByteBuf> c = clients.get(context.server());
    if (c == null) {
      c = newClient(context);
      HttpClient<ByteBuf, ByteBuf> tmp = clients.putIfAbsent(context.server(), c);
      if (tmp != null) {
        // TODO: how to shutdown a client?
        // c.shutdown();
        c = tmp;
      }
    }
    return c;
  }

  private HttpClient<ByteBuf, ByteBuf> newClient(final RequestContext context) {
    final Server server = context.server();
    final ClientConfig clientCfg = context.config();

    // User agent?
    // Client name?
    // Connection pooling?
    HttpClient<ByteBuf, ByteBuf> client = HttpClient.newClient(server.host(), server.port())
        .readTimeOut(clientCfg.readTimeout(), TimeUnit.MILLISECONDS)
        .channelOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, clientCfg.connectTimeout())
        .enableWireLogging(LogLevel.DEBUG)
        .pipelineConfigurator(new HttpDecompressionConfigurator());

    if (server.isSecure()) {
      client = client.unsafeSecure();
    }

    return client;
    // TODO
    /*HttpClient.HttpClientConfig config = new HttpClient.HttpClientConfig.Builder()
        .readTimeout(clientCfg.readTimeout(), TimeUnit.MILLISECONDS)
        .userAgent(clientCfg.userAgent())
        .build();

    PipelineConfiguratorComposite<HttpClientResponse<ByteBuf>, HttpClientRequest<ByteBuf>>
        pipelineCfg = new PipelineConfiguratorComposite<HttpClientResponse<ByteBuf>, HttpClientRequest<ByteBuf>>(
        new HttpClientPipelineConfigurator<ByteBuf, ByteBuf>(),
        new HttpDecompressionConfigurator()
    );

    HttpClientBuilder<ByteBuf, ByteBuf> builder =
        RxNetty.<ByteBuf, ByteBuf>newHttpClientBuilder(server.host(), server.port())
            .pipelineConfigurator(pipelineCfg)
            .config(config)
            .withName(clientCfg.name())
            .channelOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, clientCfg.connectTimeout());

    final int idleTimeout = clientCfg.idleConnectionsTimeoutMillis();
    if (idleTimeout == 0) {
      builder.withNoConnectionPooling();
    } else {
      builder
          .withConnectionPoolLimitStrategy(getPoolLimitStrategy(clientCfg))
          .withIdleConnectionsTimeoutMillis(idleTimeout);
    }

    if (server.isSecure()) {
      builder.withSslEngineFactory(DefaultFactories.trustAll());
    }

    return builder.build();*/
  }

  /*private PoolLimitDeterminationStrategy getPoolLimitStrategy(ClientConfig clientCfg) {
    PoolLimitDeterminationStrategy totalStrategy = poolLimits.get(clientCfg.name());
    if (totalStrategy == null) {
      totalStrategy = new MaxConnectionsBasedStrategy(clientCfg.maxConnectionsTotal());
      PoolLimitDeterminationStrategy tmp = poolLimits.putIfAbsent(clientCfg.name(), totalStrategy);
      if (tmp != null) {
        totalStrategy = tmp;
      }
    }
    return new CompositePoolLimitDeterminationStrategy(
        new MaxConnectionsBasedStrategy(clientCfg.maxConnectionsPerHost()),
        totalStrategy);
  }*/

  private List<Server> getServers(ClientConfig clientCfg) {
    List<Server> servers;
    if (clientCfg.uri().isAbsolute()) {
      servers = getServersForUri(clientCfg, clientCfg.uri());
    } else {
      servers = serverRegistry.getServers(clientCfg.vip(), clientCfg);
    }
    return servers;
  }

  private List<Server> getServersForUri(ClientConfig clientCfg, URI uri) {
    final int numRetries = clientCfg.numRetries();
    final boolean secure = "https".equals(uri.getScheme());
    List<Server> servers = new ArrayList<>();
    servers.add(new Server(uri.getHost(), getPort(uri), secure));
    for (int i = 0; i < numRetries; ++i) {
      servers.add(new Server(uri.getHost(), getPort(uri), secure));
    }
    return servers;
  }

  /** We expect UTF-8 to always be supported. */
  private static byte[] getBytes(String s) {
    try {
      return s.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Return the port taking care of handling defaults for http and https if not explicit in the
   * uri.
   */
  static int getPort(URI uri) {
    final int defaultPort = ("https".equals(uri.getScheme())) ? 443 : 80;
    return (uri.getPort() <= 0) ? defaultPort : uri.getPort();
  }

  private static class HttpDecompressionConfigurator implements Action1<ChannelPipeline> {
    @Override public void call(ChannelPipeline pipeline) {
      pipeline.addLast("deflater", new HttpContentDecompressor());
    }
  }

}
