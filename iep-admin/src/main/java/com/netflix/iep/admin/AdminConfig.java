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
package com.netflix.iep.admin;

import java.time.Duration;

/**
 * Configuration settings for tuning the admin behavior.
 */
public interface AdminConfig {

  /**
   * Default instance of the config using fixed values. Defaults are:
   *
   * <pre>
   * port          = 8077
   * backlog       = 10
   * shutdownDelay = PT0S
   * uiLocation    = /ui
   * </pre>
   */
  AdminConfig DEFAULT = new AdminConfig() {
    @Override public int port() {
      return 8077;
    }

    @Override public int backlog() {
      return 10;
    }

    @Override public Duration shutdownDelay() {
      return Duration.ZERO;
    }

    @Override public String uiLocation() {
      return "/ui";
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
