/*
 * Copyright 2014-2025 Netflix, Inc.
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
package com.netflix.iep.spring;

import com.netflix.iep.service.ClassFactory;
import com.netflix.iep.service.Service;
import com.netflix.iep.service.ServiceManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Base configuration needed for most IEP apps.
 */
@Configuration
public class IepConfiguration {

  @Bean
  ClassFactory classFactory(ApplicationContext context) {
    return new SpringClassFactory(context);
  }

  @Bean
  ServiceManager serviceManager(Set<Service> services) {
    return new ServiceManager(services);
  }

  @Bean
  Supplier<ServiceManager> serviceManagerSupplier(ApplicationContext context) {
    return () -> context.getBean(ServiceManager.class);
  }
}
