/*
 * Copyright 2014-2024 Netflix, Inc.
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
package com.netflix.iep.admin.spring;

import com.netflix.iep.admin.AdminConfig;
import com.netflix.iep.admin.AdminServer;
import com.netflix.iep.admin.EndpointMapping;
import com.netflix.iep.admin.endpoints.BaseServerEndpoint;
import com.netflix.iep.admin.endpoints.EnvEndpoint;
import com.netflix.iep.admin.endpoints.JarsEndpoint;
import com.netflix.iep.admin.endpoints.JmxEndpoint;
import com.netflix.iep.admin.endpoints.ServicesEndpoint;
import com.netflix.iep.admin.endpoints.SpectatorEndpoint;
import com.netflix.iep.admin.endpoints.SystemPropsEndpoint;
import com.netflix.iep.admin.endpoints.ThreadsEndpoint;
import com.netflix.iep.service.ServiceManager;
import com.netflix.spectator.api.NoopRegistry;
import com.netflix.spectator.api.Registry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * Configure {@link AdminServer} via Spring. The server will be setup as an eager
 * singleton, but must be explicitly started if not using a lifecycle manager with
 * guice to automatically call the {@link javax.annotation.PostConstruct} method.
 */
@Configuration
public class AdminConfiguration {

  @Bean
  AdminServer adminServer(
      Optional<AdminConfig> config,
      Set<EndpointMapping> mappings
  ) throws IOException {
    AdminConfig c = config.orElse(AdminConfig.DEFAULT);
    return new AdminServer(c, mappings);
  }

  @Bean
  EndpointMapping envEndpointMapping() {
    return new EndpointMapping("/env", new EnvEndpoint());
  }

  @Bean
  EndpointMapping sysPropsEndpointMapping() {
    return new EndpointMapping("/system", new SystemPropsEndpoint());
  }

  @Bean
  EndpointMapping jarsEndpointMapping() {
    return new EndpointMapping("/jars", new JarsEndpoint());
  }

  @Bean
  EndpointMapping jmxEndpointMapping() {
    return new EndpointMapping("/jmx", new JmxEndpoint());
  }

  @Bean
  EndpointMapping threadsEndpointMapping() {
    return new EndpointMapping("/threads", new ThreadsEndpoint());
  }

  @Bean
  EndpointMapping spectatorEndpointMapping(Optional<Registry> registry) {
    Registry r = registry.orElseGet(NoopRegistry::new);
    return new EndpointMapping("/spectator", new SpectatorEndpoint(r));
  }

  @Bean
  EndpointMapping serviceEndpointMapping(Optional<ServiceManager> manager) {
    ServiceManager sm = manager.orElseGet(() -> new ServiceManager(Collections.emptySet()));
    return new EndpointMapping("/services", new ServicesEndpoint(sm));
  }

  @Bean
  EndpointMapping baseServerEndpointMapping(Optional<Registry> registry) {
    Registry r = registry.orElseGet(NoopRegistry::new);
    return new EndpointMapping("/v1/platform/base", new BaseServerEndpoint(r));
  }

  @Bean
  EndpointMapping springBeansEndpointMapping(ApplicationContext context) {
    return new EndpointMapping("/spring-beans", new SpringBeansEndpoint(context));
  }

  @Bean
  EndpointMapping springEnvEndpointMapping(ApplicationContext context) {
    return new EndpointMapping("/spring-env", new SpringEnvEndpoint(context));
  }

  /**
   * Sample main that runs the admin with a default set of endpoints. Mostly used for
   * quick local testing of the module and common endpoints.
   */
  public static void main(String[] args) {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(AdminConfiguration.class);
    context.registerShutdownHook();
    context.refresh();
    context.start();
  }
}
