/*
 * Copyright 2014-2018 Netflix, Inc.
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
package com.netflix.iep.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.ProvisionListener;
import com.netflix.iep.service.ClassFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper for listening to injection events and invoking the PostConstruct and PreDestroy
 * annotated methods.
 */
public class LifecycleModule extends AbstractModule {

  private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleModule.class);

  private static class BindingListener implements ProvisionListener {
    private final PreDestroyList preDestroyList;

    BindingListener(PreDestroyList preDestroyList) {
      this.preDestroyList = preDestroyList;
    }

    @Override public <T> void onProvision(ProvisionInvocation<T> provisionInvocation) {
      T value = provisionInvocation.provision();
      boolean singleton = Scopes.isSingleton(provisionInvocation.getBinding());
      AnnotationUtils.invokePostConstruct(LOGGER, value, singleton ? preDestroyList : null);
    }
  }

  @Override protected void configure() {
    PreDestroyList list = new PreDestroyList();
    bindListener(Matchers.any(), new BindingListener(list));
    bind(PreDestroyList.class).toInstance(list);
    bind(ClassFactory.class).toProvider(ClassFactoryProvider.class);
  }

  @Override public boolean equals(Object obj) {
    return obj != null && getClass().equals(obj.getClass());
  }

  @Override public int hashCode() {
    return getClass().hashCode();
  }
}
