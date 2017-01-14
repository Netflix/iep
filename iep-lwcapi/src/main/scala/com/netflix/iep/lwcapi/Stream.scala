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

import com.netflix.appinfo.InstanceInfo
import com.netflix.discovery.EurekaClient
import com.netflix.iep.guice.GuiceHelper
import com.netflix.iep.json.Json
import rx.lang.scala.{Observer, Subject, Subscription}

import scala.collection.mutable


sealed trait ServerMessage

case class Server(instanceId: String, asg: String, hostname: String, port: Integer, status: InstanceInfo.InstanceStatus)
case class ServerSet(servers: Set[Server]) extends ServerMessage

case class ExpressionSubscription(expression: String, frequency: Option[Long] = None)

class Stream {
  import Stream._

  case class TrackedServer(instanceId: String,
                           uri: String,
                           expressions: List[ExpressionSubscription],
                           subscription: Option[Subscription],
                           state: Symbol,
                           nextEvent: Long)

  case class JsonSubscribe(expressions: List[ExpressionSubscription])

  case class EurekaObserver(streamId: String,
                            expressions: List[ExpressionSubscription]) extends Observer[ServerMessage] {
    private var state = mutable.Map[String, TrackedServer]()

    override def onNext(msg: ServerMessage): Unit = {
      val now = System.currentTimeMillis()
      state.synchronized {
        msg match {
          case ServerSet(servers) =>
            val announcedInstanceIds = servers.map(server => server.instanceId)
            val currentInstanceIds = state.keySet
            val newInstanceIds = announcedInstanceIds &~ currentInstanceIds
            val deadInstanceIds = currentInstanceIds &~ announcedInstanceIds

            servers.filter(s => newInstanceIds.contains(s.instanceId)).foreach(server => {
              val uri = s"http://${server.hostname}:${server.port}/lwc/api/v1/stream/$streamId"
              val tracker = TrackedServer(server.instanceId, uri, expressions, None, 'newserver, 0)
              state(server.instanceId) = tracker
            })

            deadInstanceIds.foreach(instanceId => {
              if (state.contains(instanceId)) {
                println(s"Stopping subscription for $instanceId")
                var info = state(instanceId)
                var sub = info.subscription
                if (sub.isDefined) sub.get.unsubscribe()
                state.remove(instanceId)
              }
            })
        }

        // Now, run through all the entries, and find ones which are not 'connected and whos retry
        // timer has expired.  This code is pretty gross and probably has race conditions...
        state.foreach(item => {
          val instanceId = item._1
          val info = item._2
          if (info.state != 'connected) {
            if (now >= info.nextEvent) {
              val sub = info.subscription
              if (sub.isDefined) sub.get.unsubscribe()
              val uri = URI.create(info.uri)
              val json = Json.encode(JsonSubscribe(expressions))
              println(json)
              val post = HttpClient.post(uri, List(), Some(json.getBytes))
              state(instanceId) = info.copy(subscription = Some(post.flatMap(s => s.getContentAsString).subscribe(SSEObserver(instanceId, state))), state = 'connected, nextEvent = 0)
            }
          }
        })
      }
    }
  }

  private val subject = Subject[Message]()

  case class SSEObserver(instanceId: String, state: mutable.Map[String, TrackedServer]) extends Observer[String] {
    var remainder = ""

    println(s"Subscribing to $instanceId")

    private def mapSSEType(line: String) = {
      val Array(sseType, jsonType, json) = line.split(" ", 3)
      sseType match {
        case "data:" => DataMessage(instanceId, jsonType, json)
        case "info:" => InfoMessage(instanceId, jsonType, json)
        case _ => UnknownMessage(instanceId, sseType, jsonType, json)
      }
    }

    override def onNext(msg: String): Unit = {
      val newString = remainder + msg
      var pos = newString.indexOf("\r\n")
      var index = 0
      while (pos != -1) {
        val line = newString.substring(index, pos)
        if (line.nonEmpty)
          subject.onNext(mapSSEType(line))
        index = pos + 2
        pos = newString.indexOf("\r\n", index)
      }
      remainder = newString.substring(index)
    }

    override def onError(error: Throwable): Unit = {
      state.synchronized {
        if (state.contains(instanceId)) {
          var oldstate = state(instanceId)
          // need exponential backoff tracking
          state(instanceId) = oldstate.copy(state = 'error, nextEvent = System.currentTimeMillis() + 1000)
        }
      }
      println(s"Got an onError() for $instanceId: $error")
      super.onError(error)
    }

    override def onCompleted(): Unit = {
      state.synchronized {
        if (state.contains(instanceId)) {
          var oldstate = state(instanceId)
          // need exponential backoff tracking
          state(instanceId) = oldstate.copy(state = 'error, nextEvent = System.currentTimeMillis() + 1000)
        }
      }
      println(s"Got an onCompleted() for $instanceId")
      super.onCompleted()
    }
  }

  def sseStream(client: EurekaClient,
                cluster: String,
                sseId: String,
                expressions: List[ExpressionSubscription]): Subject[Message] = {
    EurekaObservable(client, cluster + ":7001").subscribe(EurekaObserver(sseId, expressions))
    subject
  }
}

object Stream {
  abstract class Message(instanceId: String, jsonType: String, json: String)

  case class UnknownMessage(instanceId: String, sseType: String, jsonType: String, json: String)
    extends Message(instanceId, jsonType, json)

  case class DataMessage(instanceId: String, jsonType: String, json: String)
    extends Message(instanceId, jsonType, json)

  case class InfoMessage(instanceId: String, jsonType: String, json: String)
    extends Message(instanceId, jsonType, json)

  def start(client: EurekaClient, cluster: String, sseId: String, expressions: List[ExpressionSubscription]): Subject[Message] = {
    val lwcapi = new Stream
    lwcapi.sseStream(client, cluster, sseId, expressions)
  }

  private var modules = GuiceHelper.getModulesUsingServiceLoader
  private var helper = new GuiceHelper()
  helper.start(modules)
  helper.start(new EurekaModule())
  helper.addShutdownHook()

  def getEurekaClient: EurekaClient = helper.getInjector.getInstance(classOf[EurekaClient])
}
