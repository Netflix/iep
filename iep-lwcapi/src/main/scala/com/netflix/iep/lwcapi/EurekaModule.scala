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

import javax.inject.Named

import com.google.inject._
import com.netflix.appinfo.{ApplicationInfoManager, CloudInstanceConfig, EurekaInstanceConfig, InstanceInfo}
import com.netflix.config.ConfigurationManager
import com.netflix.discovery.shared.transport.jersey.Jersey1DiscoveryClientOptionalArgs
import com.netflix.discovery.{AbstractDiscoveryClientOptionalArgs, DefaultEurekaClientConfig, DiscoveryClient, EurekaClientConfig}
import org.apache.commons.configuration.Configuration

/**
  * Created by mgraff on 12/8/16.
  */
object EurekaModule {

  private class OptionalInjections {
    @Inject(optional = true)
    @Named("IEP") private val config: Configuration = null

    private def getConfig: Configuration = if (config == null) ConfigurationManager.getConfigInstance
    else config
  }

  def main(args: Array[String]) {
    System.setProperty("netflix.iep.archaius.use-dynamic", "false")
    val injector: Injector = Guice.createInjector(new EurekaModule)
  }
}

final class EurekaModule extends AbstractModule {
  protected def configure() {
    // InstanceInfo
    bind(classOf[InstanceInfo]).toProvider(classOf[InstanceInfoProvider])
    // Needs:
    // * EurekaInstanceConfig
    // * InstanceInfo
    bind(classOf[ApplicationInfoManager]).asEagerSingleton()
    // EurekaClientConfig
    bind(classOf[EurekaClientConfig]).to(classOf[DefaultEurekaClientConfig]).asEagerSingleton()
    // DiscoveryClientOptionalArgs, as of 1.6.0 we need to have an explicit binding. Using
    // Jersey1 as it is recommended by runtime team.
    bind(classOf[AbstractDiscoveryClientOptionalArgs[_]]).to(classOf[Jersey1DiscoveryClientOptionalArgs]).asEagerSingleton()
    // BackupRegistry, automatic via ImplementedBy annotation
    // Needs:
    // * InstanceInfo
    // * EurekaClientConfig
    // * DiscoveryClientOptionalArgs
    // * BackupRegistry
    bind(classOf[DiscoveryClient]).asEagerSingleton()
  }

  // Ensure that archaius configuration is setup prior to creating the eureka classes
  @Provides
  @Singleton private def provideInstanceConfig(opts: EurekaModule.OptionalInjections): EurekaInstanceConfig = new CloudInstanceConfig("netflix.appinfo.")

  override def equals(obj: Any): Boolean = obj != null && getClass == obj.getClass

  override def hashCode: Int = getClass.hashCode
}
