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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.netflix.archaius.api.config.PollingStrategy;
import com.netflix.archaius.config.AbstractConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.archaius.config.polling.PollingResponse;

/**
 * Originally from: https://github.com/Netflix/archaius/blob/2.x/archaius2-core/src/main/java/com/netflix/archaius/config/PollingDynamicConfig.java
 *
 * Imported to fix https://github.com/Netflix/archaius/issues/371
 *
 * Special DynamicConfig that reads an entire snapshot of the configuration
 * from a source and performs a delta comparison.  Each new snapshot becomes
 * the new immutable Map backing this config.  
 *
 * @author elandau
 *
 */
public class PollingDynamicConfig extends AbstractConfig {
  private static final Logger LOG = LoggerFactory.getLogger(PollingDynamicConfig.class);

  private volatile Map<String, String> current = new HashMap<>();
  private final AtomicBoolean busy = new AtomicBoolean();
  private final Callable<PollingResponse> reader;
  private final AtomicLong updateCounter = new AtomicLong();
  private final AtomicLong errorCounter = new AtomicLong();
  private final PollingStrategy strategy;

  public PollingDynamicConfig(Callable<PollingResponse> reader, PollingStrategy strategy) {
    this.reader = reader;
    this.strategy = strategy;
    strategy.execute((Runnable) this::update);
  }

  @Override
  public boolean containsKey(String key) {
    return current.containsKey(key);
  }

  @Override
  public boolean isEmpty() {
    return current.isEmpty();
  }

  @Override
  public Object getRawProperty(String key) {
    return current.get(key);
  }

  private void update() {
    // OK to ignore calls to update() if already busy updating
    if (busy.compareAndSet(false, true)) {
      updateCounter.incrementAndGet();
      try {
        PollingResponse response = reader.call();
        if (response.hasData()) {
          current = response.getToAdd();
          notifyConfigUpdated(this);
        }
      }
      catch (Exception e) {
        errorCounter.incrementAndGet();
        try {
          notifyError(e, this);
        }
        catch (Exception e2) {
          LOG.warn("Failed to notify error observer", e2);
        }
        throw new RuntimeException(e);
      }
      finally {
        busy.set(false);
      }
    }
  }

  public void shutdown() {
    strategy.shutdown();
  }

  public long getUpdateCounter() {
    return updateCounter.get();
  }

  public long getErrorCounter() {
    return errorCounter.get();
  }

  @Override
  public Iterator<String> getKeys() {
    return current.keySet().iterator();
  }
}
