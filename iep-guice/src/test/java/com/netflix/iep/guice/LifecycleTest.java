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
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@RunWith(JUnit4.class)
public class LifecycleTest {

  @Test
  public void justInTimeBinding() throws Exception {
    GuiceHelper helper = new GuiceHelper();
    helper.start();
    Injector injector = helper.getInjector();

    AutoStateObject obj = injector.getInstance(AutoStateObject.class);
    Assert.assertEquals(State.STARTED, obj.getState());

    helper.shutdown();
    Assert.assertEquals(State.STOPPED, obj.getState());
  }

  @Test
  public void justInTimeNotSingletonBinding() throws Exception {
    GuiceHelper helper = new GuiceHelper();
    helper.start();
    Injector injector = helper.getInjector();

    StateObject obj = injector.getInstance(StateObject.class);
    Assert.assertEquals(State.STARTED, obj.getState());

    helper.shutdown();

    // Since it is not a singleton, caller is responsible for the object to avoid
    // a leak due to keeping track objects that could be frequently created
    Assert.assertEquals(State.STARTED, obj.getState());
  }

  @Test
  public void explicitBinding() throws Exception {
    GuiceHelper helper = new GuiceHelper();
    helper.start(new AbstractModule() {
      @Override protected void configure() {
        bind(StateObject.class).asEagerSingleton();
      }
    });
    Injector injector = helper.getInjector();

    StateObject obj = injector.getInstance(StateObject.class);
    Assert.assertEquals(State.STARTED, obj.getState());

    helper.shutdown();
    Assert.assertEquals(State.STOPPED, obj.getState());
  }

  @Test
  public void autoCloseable() throws Exception {
    GuiceHelper helper = new GuiceHelper();
    helper.start(new AbstractModule() {
      @Override protected void configure() {
        bind(AutoStateObject.class).asEagerSingleton();
      }
    });
    Injector injector = helper.getInjector();

    AutoStateObject obj = injector.getInstance(AutoStateObject.class);
    Assert.assertEquals(State.STARTED, obj.getState());

    helper.shutdown();
    Assert.assertEquals(State.STOPPED, obj.getState());
  }

  @Test
  public void provides() throws Exception {
    GuiceHelper helper = new GuiceHelper();
    helper.start(new AbstractModule() {
      @Override protected void configure() {
      }

      @Provides
      @Singleton
      private StateObject providesStateObject() {
        return new StateObject();
      }
    });
    Injector injector = helper.getInjector();

    StateObject obj = injector.getInstance(StateObject.class);
    Assert.assertEquals(State.STARTED, obj.getState());

    helper.shutdown();
    Assert.assertEquals(State.STOPPED, obj.getState());
  }

  @Test
  public void provider() throws Exception {
    GuiceHelper helper = new GuiceHelper();
    helper.start(new AbstractModule() {
      @Override protected void configure() {
        bind(StateObject.class).toProvider(StateObjectProvider.class).in(Scopes.SINGLETON);
      }
    });
    Injector injector = helper.getInjector();

    StateObject obj = injector.getInstance(StateObject.class);
    Assert.assertEquals(State.STARTED, obj.getState());

    helper.shutdown();
    Assert.assertEquals(State.STOPPED, obj.getState());
  }

  private enum State {
    INIT, STARTED, STOPPED
  }

  private static class StateObject {

    private State state;

    @Inject
    StateObject() {
      state = State.INIT;
    }

    @PostConstruct
    private void start() {
      state = State.STARTED;
    }

    @PreDestroy
    private void stop() {
      state = State.STOPPED;
    }

    State getState() {
      return state;
    }
  }

  private static class StateObjectProvider implements Provider<StateObject> {
    @Override public StateObject get() {
      return new StateObject();
    }
  }

  @Singleton
  private static class AutoStateObject implements AutoCloseable {

    private State state;

    @Inject
    AutoStateObject() {
      state = State.STARTED;
    }

    @Override public void close() throws Exception {
      state = State.STOPPED;
    }

    State getState() {
      return state;
    }
  }
}
