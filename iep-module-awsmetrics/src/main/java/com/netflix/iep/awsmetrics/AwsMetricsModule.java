/*
 * Copyright 2014-2021 Netflix, Inc.
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
package com.netflix.iep.awsmetrics;

import com.amazonaws.metrics.AwsSdkMetrics;
import com.amazonaws.metrics.MetricCollector;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Spectator;
import com.netflix.spectator.aws.SpectatorMetricCollector;

import javax.inject.Singleton;

/**
 * @deprecated Migrate to AWS SDK for Java V2. The {@code spectator-ext-aws2} library
 * can be used and will be loaded automatically by the V2 SDK if it is present on the
 * classpath.
 */
@Deprecated
public final class AwsMetricsModule extends AbstractModule {

  @Override protected void configure() {
    requestStaticInjection(StaticInject.class);
  }

  @Provides
  @Singleton
  private MetricCollector provideMetricCollector(OptionalInjections opts) {
    return new SpectatorMetricCollector(opts.getRegistry());
  }

  @Override public boolean equals(Object obj) {
    return obj != null && getClass().equals(obj.getClass());
  }

  @Override public int hashCode() {
    return getClass().hashCode();
  }

  private static class OptionalInjections {
    @Inject(optional = true)
    private Registry registry;

    Registry getRegistry() {
      return (registry == null) ? Spectator.globalRegistry() : registry;
    }
  }

  private static class StaticInject {
    @Inject
    static void initializeMetricCollector(MetricCollector collector) {
      AwsSdkMetrics.setMetricCollector(collector);
    }
  }
}
