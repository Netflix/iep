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

import com.netflix.iep.json.Json
import com.typesafe.scalalogging.StrictLogging
import rx.lang.scala.Observable

import scala.util.control.NonFatal
import scalaj.http._

case class EddaAutoScalingGroup(name: String,
                                start: Long,
                                slot: Int)
case class EddaInstance(instanceId: String,
                        vpcId: String,
                        publicDnsName: Option[String],
                        publicIpAddress: Option[String],
                        privateIpAddress: String,
                        availabilityZone: String,
                        subnetId: String,
                        instanceType: String,
                        lifecycleState: String,
                        imageId: String,
                        launchTime: Long,
                        platform: Option[String],
                        start: Long,
                        slot: Long)
case class EddaClustersResponse(name: String,
                                start: Long,
                                autoScalingGroups: List[EddaAutoScalingGroup],
                                instances: List[EddaInstance])

//
// Return a set of instances we should try connecting to.
// As we do not send to these nodes, we only collect data from them,
// it's important we connect before they are accepting client traffic
// so we don't drop data.
//
class EddaObservable(uri: URI) extends StrictLogging {
  private def makeRequest() = {
    val headers = Map(
      "Accept" -> "application/json",
      "X-Netflix.client.cluster.name" -> sys.env.getOrElse("NETFLIX_CLUSTER", "local"),
      "X-Netflix.environment" -> sys.env.getOrElse("NETFLIX_ENVIRONMENT", "local"),
      "X-Netflix.client.asg.name" -> sys.env.getOrElse("NETFLIX_AUTO_SCALE_GROUP", "local"),
      "X-Netflix.client.instid" -> sys.env.getOrElse("EC2_INSTANCE_ID", "local"),
      "X-Netflix.client.az" -> sys.env.getOrElse("EC2_AVAILABILITY_ZONE", "none"),
      "X-Netflix.client.region" -> sys.env.getOrElse("EC2_REGION", "none")
    )
    Http(uri.toString).headers(headers)
  }

  private def parseJson(text: String): Set[Server] = {
    val response = Json.decode[EddaClustersResponse](text)
    if (response.instances == null)
      throw new RuntimeException("instances field in JSON response is missing")
    response.instances.map { i =>
      if (i.instanceId == null)
        throw new RuntimeException("instance instanceId is null")
      if (i.privateIpAddress == null)
        throw new RuntimeException("instance privateIpAddress is null")
      Server(i.instanceId, i.privateIpAddress, 7001)
    }.toSet
  }

  var thread: Thread = _

  def start(): Observable[ServerMessage] = {
    // request is immutable
    val request = makeRequest()

    Observable(subscriber => {
      var previousSet: Set[Server] = Set()

      thread = new Thread(new Runnable() {
        private def poll(): Set[Server] = {
          val serverSet: Set[Server] = try {
            val response = request.asString
            val contentType = response.header("Content-Type").getOrElse("unset")
            if (response.code != 200) {
              logger.warn(s"edda fetch returned http status ${response.code}")
              previousSet
            } else if (contentType != "application/json") {
              logger.warn(s"Response is not application/json: $contentType")
              previousSet
            } else {
              parseJson(response.body)
            }
          } catch {
            case NonFatal(e) =>
              logger.warn("edda", e)
              previousSet
          }

          previousSet = serverSet
          previousSet
        }

        def run(): Unit = {
          while (!subscriber.isUnsubscribed) {
            var info = poll()
            subscriber.onNext(ServerSet(info))
            Thread.sleep(10000)
          }
          if (!subscriber.isUnsubscribed) {
            subscriber.onError(new RuntimeException("Edda thread exited while still subscribed"))
          }
        }
      })
      thread.start()
    })
  }
}

object EddaObservable {
  def apply(uri: URI) = new EddaObservable(uri: URI)
}
