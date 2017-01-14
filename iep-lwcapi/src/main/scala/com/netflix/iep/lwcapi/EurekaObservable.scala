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

import com.netflix.appinfo._
import com.netflix.discovery._
import rx.lang.scala.Observable

import scala.collection.JavaConverters._

//
// Return a set of instances we should try connecting to.  We will report
// all instances which are not marked as OUT_OF_SERVICE, since an instance
// which is just starting up may still receive client traffic.  As we
// do not send to these nodes, we only collect data from them, it's important
// we connect before they are accepting client traffic so we don't drop data.
//
object EurekaObservable {
  def apply(client: EurekaClient, vip: String): Observable[ServerMessage] = {
    Observable(subscriber => {
      new Thread(new Runnable() {
        private def poll(): Set[Server] = {
          val info = client.getInstancesByVipAddress(vip, false).asScala
            .filter(x => x.getStatus != InstanceInfo.InstanceStatus.OUT_OF_SERVICE)
            .map(x => Server(x.getInstanceId, x.getASGName, x.getHostName, x.getPort, x.getStatus))
          info.toSet
        }

        def run() = {
          while (!subscriber.isUnsubscribed) {
            var info = poll()
            subscriber.onNext(ServerSet(info))
            Thread.sleep(10000)
          }
          if (!subscriber.isUnsubscribed) {
            subscriber.onError(new RuntimeException("Eureka thread exited while still subscribed"))
          }
        }
      }).start()
    })
  }
}
