/*
 * Copyright 2014-2022 Netflix, Inc.
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
package com.netflix.iep.userservice;

import com.netflix.iep.admin.EndpointMapping;
import com.netflix.spectator.api.NoopRegistry;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.ipc.http.HttpClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.net.ssl.SSLSocketFactory;
import java.util.Optional;
import java.util.Set;

/**
 * Setup bindings for {@link UserService}.
 */
@Configuration
public class UserServiceConfiguration {

  @Bean
  Context context(
      Optional<Registry> registry,
      Optional<Config> config,
      Optional<HttpClient> client,
      Optional<SSLSocketFactory> factory) {
    Registry r = registry.orElseGet(NoopRegistry::new);
    Config c = config.orElseGet(UserServiceConfiguration::defaultConfig);
    HttpClient h = client.orElse(HttpClient.DEFAULT_CLIENT);
    SSLSocketFactory f = factory.orElse(null);
    return new Context(r, c, h, f);
  }

  @Bean
  WhitelistUserService whitelistUserService(Context context) {
    return new WhitelistUserService(context);
  }

  @Bean
  HttpUserService httpUserService(Context context) {
    return new HttpUserService(context);
  }

  @Bean
  @Primary
  UserService userService(Set<UserService> services) {
    return new CompositeUserService(services);
  }

  @Bean
  EndpointMapping userServiceEndpoint(UserService service) {
    return new EndpointMapping("/userservice", new UserServiceEndpoint(service));
  }

  private static Config defaultConfig() {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) {
      cl = UserServiceConfiguration.class.getClassLoader();
    }
    return ConfigFactory.load(cl);
  }
}
