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
package com.netflix.iep.archaius1;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.util.Modules;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.bridge.StaticArchaiusBridgeModule;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DeploymentContext;
import org.apache.commons.configuration.Configuration;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Helper for configuring archaius v1.
 */
public final class Archaius1Module extends AbstractModule {

  @Override protected void configure() {
    install(Modules.override(new StaticArchaiusBridgeModule()).with(new OverrideModule()));
  }

  private static class OverrideModule extends AbstractModule {

    @Override protected void configure() {
    }

    @Provides
    @Singleton
    private DeploymentContext providesDeploymentContext(Config config) {
      return new DeploymentContext() {
        @Override
        public String getDeploymentEnvironment() {
          return getValue(ContextKey.environment);
        }

        @Override
        public void setDeploymentEnvironment(String s) {
        }

        @Override
        public String getDeploymentDatacenter() {
          return getValue(ContextKey.datacenter);
        }

        @Override
        public void setDeploymentDatacenter(String s) {
        }

        @Override
        public String getApplicationId() {
          return getValue(ContextKey.appId);
        }

        @Override
        public void setApplicationId(String s) {
        }

        @Override
        public String getDeploymentServerId() {
          return getValue(ContextKey.serverId);
        }

        @Override
        public void setDeploymentServerId(String s) {
        }

        @Override
        public String getDeploymentStack() {
          return getValue(ContextKey.stack);
        }

        @Override
        public void setDeploymentStack(String s) {
        }

        @Override
        public String getDeploymentRegion() {
          return getValue(ContextKey.region);
        }

        @Override
        public void setDeploymentRegion(String s) {
        }

        @Override
        public String getValue(ContextKey contextKey) {
          String s = contextKey.name();
          return config.getString("netflix.iep.archaius.scope." + s);
        }

        @Override
        public void setValue(ContextKey contextKey, String s) {
        }
      };
    }
  }

  @Provides
  @Singleton
  @Named("IEP")
  private Configuration providesConfiguration() {
    return ConfigurationManager.getConfigInstance();
  }

  @Override public boolean equals(Object obj) {
    return obj != null && getClass().equals(obj.getClass());
  }

  @Override public int hashCode() {
    return getClass().hashCode();
  }
}
