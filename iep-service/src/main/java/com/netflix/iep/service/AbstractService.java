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
package com.netflix.iep.service;

import javax.annotation.PostConstruct;

/**
 * Base class that manages the service state. Just implement the {@link #startImpl()} and
 * {@link #stopImpl()}.
 */
public abstract class AbstractService implements Service, AutoCloseable {

  private final String name;
  private volatile State state;

  public AbstractService() {
    this(null);
  }

  public AbstractService(String name) {
    this.name = (name == null) ? getClass().getSimpleName() : name;
    this.state = State.NEW;
  }

  @Override public final String name() {
    return name;
  }

  @Override public boolean isHealthy() {
    return state == State.RUNNING;
  }

  @Override public final State state() {
    return state;
  }

  @PostConstruct
  public final synchronized void start() throws Exception {
    if (state != State.NEW) {
      throw new IllegalStateException("attempting to start service '" +
          name + "' from state " + state.name());
    } else {
      state = State.STARTING;
      try {
        startImpl();
        state = State.RUNNING;
      } catch (Exception e) {
        state = State.FAILED;
        throw e;
      }
    }
  }

  public final synchronized void stop() throws Exception {
    if (state == State.STARTING || state == State.RUNNING) {
      state = State.STOPPING;
      try {
        stopImpl();
        state = State.TERMINATED;
      } catch (Exception e) {
        state = State.FAILED;
        throw e;
      }
    }
  }

  /**
   * Equivalent to calling {@link #stop()}. This method is used for lifecycle management
   * with DI frameworks.
   */
  @Override public void close() throws Exception {
    stop();
  }

  protected abstract void startImpl() throws Exception;
  protected abstract void stopImpl() throws Exception;
}
