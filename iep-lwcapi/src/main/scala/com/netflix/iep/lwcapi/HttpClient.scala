/*
 * Copyright 2014-2017 Netflix, Inc.
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
package com.netflix.iep.lwcapi

import java.net.URI
import java.nio.charset.Charset
import io.netty.buffer.ByteBuf
import io.reactivex.netty.client.{MaxConnectionsBasedStrategy, RxClient}
import io.reactivex.netty.pipeline.ssl.DefaultFactories
import io.reactivex.netty.protocol.http.client.{CompositeHttpClient, CompositeHttpClientBuilder, HttpClientPipelineConfigurator, HttpClientRequest, HttpClientResponse => JavaHttpClientResponse, HttpResponseHeaders => JavaHttpResponseHeaders}
import rx.lang.scala.JavaConversions._
import rx.lang.scala.Observable
import rx.{Observable => JavaObservable}

import scala.collection.JavaConverters._

trait HttpClient {
  def get(path: String,
          headers: Seq[(String, String)] = List(),
          body: Option[Array[Byte]] = None): Observable[HttpClientResponse]

  def post(path: String,
           headers: Seq[(String, String)] = List(),
           body: Option[Array[Byte]] = None): Observable[HttpClientResponse]
}

final case class HttpClientImpl(serverInfo: ServerInfo) extends HttpClient {
  def get(path: String,
          headers: Seq[(String, String)] = List(),
          body: Option[Array[Byte]] = None): Observable[HttpClientResponse] =
  {
    submitRequest(HttpClientRequest.createGet(path), headers, body)
  }

  def post(path: String,
           headers: Seq[(String, String)] = List(),
           body: Option[Array[Byte]] = None): Observable[HttpClientResponse] =
  {
    submitRequest(HttpClientRequest.createPost(path), headers, body)
  }

  private def submitRequest(request: HttpClientRequest[ByteBuf],
                            headers: Seq[(String, String)] = List(),
                            body: Option[Array[Byte]] = None): Observable[HttpClientResponse] =
  {
    for (header <- headers) { request.withHeader(header._1, header._2) }
    for (b <- body) { request.withContent(b) }
    val client = if (serverInfo.isSecure) HttpClient.secureGlobalClient else HttpClient.globalClient
    convertResponse(client.submit(serverInfo.getNettyServerInfo, request))
  }

  private def convertResponse(response: JavaObservable[JavaHttpClientResponse[ByteBuf]]): Observable[HttpClientResponse] = {
    toScalaObservable(response).map(r => {
      HttpClientResponse(
        response = r,
        headers  = HttpResponseHeaders(r.getHeaders),
        status   = r.getStatus.code()
      )
    })
  }

}

object HttpClient {
  private var factory: URI => HttpClient = (uri) => HttpClientImpl(ServerInfo(uri))

  val globalClient: CompositeHttpClient[ByteBuf, ByteBuf] = new CompositeHttpClientBuilder[ByteBuf, ByteBuf]()
    .withMaxConnections(MaxConnectionsBasedStrategy.DEFAULT_MAX_CONNECTIONS)
    .pipelineConfigurator(new HttpClientPipelineConfigurator())
    .build()

  val secureGlobalClient: CompositeHttpClient[ByteBuf, ByteBuf] = new CompositeHttpClientBuilder[ByteBuf, ByteBuf]()
    .withMaxConnections(MaxConnectionsBasedStrategy.DEFAULT_MAX_CONNECTIONS)
    .withSslEngineFactory(DefaultFactories.trustAll())
    .build()

  def apply(uri: URI): HttpClient = {
    factory(uri)
  }

  def get(uri: URI,
          headers: Seq[(String, String)] = List(),
          body: Option[Array[Byte]] = None): Observable[HttpClientResponse] =
  {
    HttpClient(uri).get(uri.getPath, headers, body)
  }

  def post(uri: URI,
           headers: Seq[(String, String)] = List(),
           body: Option[Array[Byte]] = None): Observable[HttpClientResponse] =
  {
    HttpClient(uri).post(uri.getPath, headers, body)
  }
}

final case class ServerInfo(uri: URI) {
  def getNettyServerInfo: RxClient.ServerInfo = {
    lazy val defaultPort: Int = if (isSecure) 443 else 80
    val port = if (uri.getPort <= 0) defaultPort else uri.getPort
    new RxClient.ServerInfo(uri.getHost, port)
  }

  def isSecure: Boolean = uri.getScheme == "https"
}

final case class HttpResponseHeaders(headers: JavaHttpResponseHeaders) {
  def contains(key: String): Boolean = {
    headers.contains(key)
  }

  def get(key: String): Option[String] = {
    Option(headers.get(key))
  }

  def getAll(key: String): List[String] = {
    headers.getAll(key).asScala.toList
  }

}

object StringDecoder {
  def apply()(in: ByteBuf): String = { in.toString(Charset.defaultCharset) }
}

final case class HttpClientResponse(response: JavaHttpClientResponse[ByteBuf],
                                    headers: HttpResponseHeaders,
                                    status: Int)
{
  def getContent: Observable[ByteBuf] = {
    toScalaObservable(response.getContent)
  }

  def getContentAsString: Observable[String] = {
    getContent.map(StringDecoder())
  }

  def ignoreContent: HttpClientResponse = {
    response.ignoreContent()
    this
  }
}
