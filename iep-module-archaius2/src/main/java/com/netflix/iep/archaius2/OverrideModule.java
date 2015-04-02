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
public class OverrideModule extends AbstractModule {
  @Override protected void configure() {
    Module m = Modules
        .override(new com.netflix.archaius.guice.ArchaiusModule())
        .with(new ArchaiusModule());
    install(m);
  }
}
