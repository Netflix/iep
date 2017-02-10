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

object Main extends App {
  val expressions = List(ExpressionSubscription(":true,(,name,nf.node,nf.cluster,),:by", Some(0)))
  val streamName = "foobar"

  val eddaUri = new URI(sys.env.getOrElse("LWCAPI_EDDA_URL", "http://localhost:7001"))

  val stream = Stream.start(eddaUri, streamName, expressions).subscribe(s => println(s))
}
