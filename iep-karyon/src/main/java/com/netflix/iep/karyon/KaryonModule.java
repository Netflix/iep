package com.netflix.iep.karyon;


import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.config.ConfigurationManager;
import netflix.adminresources.resources.KaryonWebAdminModule;
import netflix.karyon.health.AlwaysHealthyHealthCheck;
import netflix.karyon.health.HealthCheckHandler;
import netflix.karyon.health.HealthCheckInvocationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.TimeoutException;


public class KaryonModule extends KaryonWebAdminModule {

  private static final Logger LOGGER = LoggerFactory.getLogger(KaryonModule.class);

  private void loadProperties(String name) {
    try {
      ConfigurationManager.loadCascadedPropertiesFromResources(name);
    } catch (IOException e) {
      LOGGER.warn("failed to load properties for '" + name + "'");
    }
  }

  @Override protected void configure() {
    loadProperties("iep-karyon");
    super.configure();
  }

  @Provides @Singleton
  public HealthCheckHandler provideHealthCheckHandler() {
    return new AlwaysHealthyHealthCheck();
  }

  @Provides @Singleton
  public HealthCheckInvocationStrategy
  provideHealthCheckInvocationStrategy(final HealthCheckHandler handler) {
    return new HealthCheckInvocationStrategy() {

      @Override
      public int invokeCheck() throws TimeoutException {
        return handler.getStatus();
      }

      @Override
      public HealthCheckHandler getHandler() {
        return handler;
      }
    };
  }
}
