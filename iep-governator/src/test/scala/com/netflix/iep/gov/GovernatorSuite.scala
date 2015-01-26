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
package com.netflix.iep.gov

import com.google.inject.Binder
import com.google.inject.Module
import org.scalatest.FunSuite

class GovernatorSuite extends FunSuite {

  import scala.collection.JavaConversions._

  System.setProperty("GovernatorSuite.test1", c("Module1"))
  System.setProperty("GovernatorSuite.test2", c("Module1", "Module2", "Module3"))
  System.setProperty("GovernatorSuite.test3", "BadClassName")
  System.setProperty("GovernatorSuite.test4", "none")
  System.setProperty("GovernatorSuite.test5", s"${c("Module1")},service-loader")

  private def c(vs: String*): String = vs.map(s => s"com.netflix.iep.gov.$s").mkString(",")

  test("modules system prop: one") {
    val expected = List(new Module1)
    assert(Governator.getModulesUsingSystemProp("GovernatorSuite.test1").toList === expected)
  }

  test("modules system prop: multi") {
    val expected = List(new Module1, new Module2, new Module3)
    assert(Governator.getModulesUsingSystemProp("GovernatorSuite.test2").toList === expected)
  }

  test("modules system prop: bad class") {
    intercept[ClassNotFoundException] {
      Governator.getModulesUsingSystemProp("GovernatorSuite.test3")
    }
  }

  test("modules system prop: none") {
    val expected = Nil
    assert(Governator.getModulesUsingSystemProp("GovernatorSuite.test4").toList === expected)
  }

  test("modules system prop: service-loader") {
    val expected = List(new Module3)
    assert(Governator.getModulesUsingSystemProp("foo").toList === expected)
  }

  test("modules system prop: explicit with service-loader") {
    val expected = List(new Module1, new Module3)
    assert(Governator.getModulesUsingSystemProp("GovernatorSuite.test5").toList === expected)
  }
}

class Module1 extends Module {
  override def configure(binder: Binder): Unit = ???
  override def equals(obj: Any): Boolean = obj.isInstanceOf[Module1]
}

class Module2 extends Module {
  override def configure(binder: Binder): Unit = ???
  override def equals(obj: Any): Boolean = obj.isInstanceOf[Module2]
}

class Module3 extends Module {
  override def configure(binder: Binder): Unit = ???
  override def equals(obj: Any): Boolean = obj.isInstanceOf[Module3]
}
