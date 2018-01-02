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
package com.netflix.iep.platformservice;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.netflix.archaius.api.config.PollingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.archaius.util.Futures;
import com.netflix.archaius.util.ThreadFactories;

/**
 * Originally from: https://github.com/Netflix/archaius/blob/2.x/archaius2-core/src/main/java/com/netflix/archaius/config/polling/FixedPollingStrategy.java
 *
 * Imported to fix https://github.com/Netflix/archaius/pull/370
 */
public class FixedPollingStrategy implements PollingStrategy {
  private static final Logger LOG = LoggerFactory.getLogger(FixedPollingStrategy.class);

  private final ScheduledExecutorService executor;
  private final long interval;
  private final TimeUnit units;
  private final boolean syncInit;

  public FixedPollingStrategy(long interval, TimeUnit units) {
    this(interval, units, true);
  }

  public FixedPollingStrategy(long interval, TimeUnit units, boolean syncInit) {
    this.executor = Executors.newSingleThreadScheduledExecutor(ThreadFactories.newNamedDaemonThreadFactory("Archaius-Poller-%d"));
    this.interval = interval;
    this.units    = units;
    this.syncInit = syncInit;
  }

  @Override
  public Future<?> execute(final Runnable callback) {
    while (syncInit) {
      try {
        callback.run();
        break;
      } catch (Exception e) {
        LOG.warn("sync-init, failed to load properties", e);
        try {
          units.sleep(interval);
        }
        catch (InterruptedException e1) {
          Thread.currentThread().interrupt();
          return Futures.immediateFailure(e);
        }
      }
    }
    return executor.scheduleWithFixedDelay((Runnable) () -> {
      try {
        callback.run();
      } catch (Exception e) {
        LOG.warn("failed to load properties", e);
      }
    }, interval, interval, units);
  }

  @Override
  public void shutdown() {
    executor.shutdown();
  }

}
