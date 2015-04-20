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
package com.netflix.iep.archaius2;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.util.Modules;

/**
 * Work around for overriding the AppConfig, can be removed when we get a release of archaius
 * with:
 * https://github.com/Netflix/archaius/issues/286
 * https://github.com/Netflix/archaius/pull/287
 */
public final class OverrideModule extends AbstractModule {
  @Override protected void configure() {
    Module m = Modules
        .override(new com.netflix.archaius.guice.ArchaiusModule())
        .with(new ArchaiusModule());
    install(m);
  }
}
