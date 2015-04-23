/*
 * Copyright 2015 Netflix, Inc.
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
package com.netflix.iep.platformservice;

import com.netflix.archaius.persisted2.ScopePredicate;

import java.util.Map;
import java.util.Set;

/**
 * Global properties are heavily abused internally and should be avoided. This wraps another
 * predicate and ensures that at least one of appId, cluster, or asg is set. That presumes that
 * all three will be set as part of the configured scope.
 */
public class NoGlobalPredicate implements ScopePredicate {

  private static final String[] REQUIRED = {"appId", "cluster", "asg"};

  private final ScopePredicate wrapped;

  public NoGlobalPredicate(ScopePredicate wrapped) {
    this.wrapped = wrapped;
  }

  @Override public boolean evaluate(Map<String, Set<String>> map) {
    for (String k : REQUIRED) {
      Set<String> vs = map.get(k);
      if (vs != null && !vs.isEmpty()) {
        return wrapped.evaluate(map);
      }
    }
    return false;
  }
}
