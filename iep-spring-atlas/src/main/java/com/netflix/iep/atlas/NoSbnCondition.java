/*
 * Copyright 2014-2024 Netflix, Inc.
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
package com.netflix.iep.atlas;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * If the property for the internal SBN bindings is present, then disable the configuration.
 * Helps mitigate issues with poor dependency hygiene.
 */
class NoSbnCondition implements Condition {
  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    String sbn2 = context.getEnvironment().getProperty("management.metrics.export.atlas.enabled");
    String sbn3 = context.getEnvironment().getProperty("management.atlas.metrics.export.enabled");
    return sbn2 == null && sbn3 == null;
  }
}
