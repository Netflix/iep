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
package com.netflix.iep.admin.guice;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.netflix.iep.admin.HttpEndpoint;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Endpoint that provides a list of keys available via the guice injector.
 */
@Singleton
public class GuiceEndpoint implements HttpEndpoint {

  private final Injector injector;

  @Inject
  public GuiceEndpoint(Injector injector) {
    this.injector = injector;
  }

  @Override public Object get() {
    return getKeySet(v -> true);
  }

  @Override public Object get(String path) {
    Pattern p = Pattern.compile(path);
    return getKeySet(v -> p.matcher(v).find());
  }

  private boolean isMultibind(Key<?> key) {
    Class<?> t = key.getTypeLiteral().getRawType();
    Class<?> a = key.getAnnotationType();
    return Collection.class.isAssignableFrom(t)
        || Map.class.isAssignableFrom(t)
        || (a != null && a.getName().startsWith("com.google.inject.multibindings"));
  }

  private String keyString(Key<?> key) {
    Class<?> t = key.getTypeLiteral().getRawType();
    Class<?> a = key.getAnnotationType();
    return t.getName() + (a == null ? "" : ":" + a.getName());
  }

  Map<String, Key<?>> getBindingKeys(Predicate<String> p) {
    Map<Key<?>, Binding<?>> bindings = injector.getAllBindings();
    Map<String, Key<?>> keys = bindings.keySet().stream()
        .filter(k -> !isMultibind(k) && p.test(keyString(k)))
        .collect(Collectors.toMap(this::keyString, k -> k));
    return new TreeMap<>(keys);
  }

  Set<String> getKeySet(Predicate<String> p) {
    return getBindingKeys(p).keySet();
  }
}
