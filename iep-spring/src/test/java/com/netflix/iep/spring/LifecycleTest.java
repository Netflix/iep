/*
 * Copyright 2014-2023 Netflix, Inc.
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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@RunWith(JUnit4.class)
public class LifecycleTest {

  private AnnotationConfigApplicationContext createContext() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.setAllowBeanDefinitionOverriding(false);
    context.registerBean(SpringClassFactory.class);
    return context;
  }

  @Test
  public void autoCloseable() throws Exception {
    AutoStateObject obj;

    try (AnnotationConfigApplicationContext context = createContext()) {
      context.registerBean(AutoStateObject.class);
      context.refresh();
      context.start();

      obj = context.getBean(AutoStateObject.class);
      Assert.assertEquals(State.STARTED, obj.getState());
    }

    Assert.assertEquals(State.STOPPED, obj.getState());
  }

  @Test
  public void prototypeBinding() throws Exception {
    StateObject obj;

    try (AnnotationConfigApplicationContext context = createContext()) {
      context.registerBean(StateObject.class);
      context.refresh();
      context.start();

      obj = context.getBean(StateObject.class);
      Assert.assertEquals(State.STARTED, obj.getState());
    }

    // Since it is not a singleton, caller is responsible for the object to avoid
    // a leak due to keeping track objects that could be frequently created
    Assert.assertEquals(State.STARTED, obj.getState());
  }

  @Test
  public void explicitBinding() throws Exception {
    StateObject obj;

    try (AnnotationConfigApplicationContext context = createContext()) {
      context.registerBean(StateObject.class, bd -> bd.setScope("singleton"));
      context.refresh();
      context.start();

      obj = context.getBean(StateObject.class);
      Assert.assertEquals(State.STARTED, obj.getState());
    }

    Assert.assertEquals(State.STOPPED, obj.getState());
  }

  @Test
  public void provides() throws Exception {
    StateObject obj;

    try (AnnotationConfigApplicationContext context = createContext()) {
      context.register(StateObjectConfiguration.class);
      context.refresh();
      context.start();

      obj = context.getBean(StateObject.class);
      Assert.assertEquals(State.STARTED, obj.getState());
    }

    Assert.assertEquals(State.STOPPED, obj.getState());
  }

  @Test
  public void provider() throws Exception {
    StateObject obj;

    try (AnnotationConfigApplicationContext context = createContext()) {
      context.registerBean(StateObject.class, StateObject::new, bd -> bd.setScope("singleton"));
      context.refresh();
      context.start();

      obj = context.getBean(StateObject.class);
      Assert.assertEquals(State.STARTED, obj.getState());
    }

    Assert.assertEquals(State.STOPPED, obj.getState());
  }

  @Test
  public void postConstructCalledBeforeInjecting() throws Exception {
    InjectedStateObject obj;

    try (AnnotationConfigApplicationContext context = createContext()) {
      context.registerBean(StateObject.class, StateObject::new, bd -> bd.setScope("singleton"));
      context.registerBean(InjectedStateObject.class);
      context.refresh();
      context.start();

      obj = context.getBean(InjectedStateObject.class);
      Assert.assertEquals(State.STARTED, obj.getState());
    }

    Assert.assertEquals(State.STOPPED, obj.getState());
  }

  private enum State {
    INIT, STARTED, STOPPED
  }

  @Configuration
  public static class StateObjectConfiguration {

    @Bean
    StateObject stateObject() {
      return new StateObject();
    }
  }

  @Scope("prototype")
  private static class StateObject {

    private State state;

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

  private static class AutoStateObject implements AutoCloseable {

    private State state;

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


  private static class InjectedStateObject {

    private StateObject obj;

    InjectedStateObject(StateObject obj) {
      this.obj = obj;
      Assert.assertEquals(State.STARTED, obj.getState());
    }

    State getState() {
      return obj.getState();
    }
  }
}
