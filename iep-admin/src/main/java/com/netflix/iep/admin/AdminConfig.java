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
package com.netflix.iep.admin;

import com.netflix.iep.config.ConfigManager;
import com.typesafe.config.Config;

import java.time.Duration;

/**
 * Configuration settings for tuning the admin behavior.
 */
public interface AdminConfig {

  /**
   * Default instance of the config using values from reference config.
   */
  AdminConfig DEFAULT = new AdminConfig() {
    private final Config cfg = ConfigManager.get().getConfig("netflix.iep.admin");

    @Override public int port() {
      return cfg.getInt("port");
    }

    @Override public int backlog() {
      return cfg.getInt("backlog");
    }

    @Override public Duration shutdownDelay() {
      return cfg.getDuration("shutdown-delay");
    }

    @Override public String uiLocation() {
      return cfg.getString("ui-location");
    }
  };

  /** Port to use for the admin server. */
  int port();

  /** Backlog setting to use for the socket. */
  int backlog();

  /** How long to wait for pending requests when shutting down. */
  Duration shutdownDelay();

  /**
   * Location to redirect to for the UI. If the user hits the server with a path of {@code /},
   * {@code /baseserver}, or {@code /admin}, then a redirect will be returned using the location
   * set here.
   */
  String uiLocation();
}
