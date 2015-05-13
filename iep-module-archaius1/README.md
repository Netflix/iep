
## Description

Guice module to configure [archaius1](https://github.com/Netflix/archaius). This module will
primarily setup the [archaius2-archaius1-bridge](https://github.com/Netflix/archaius/tree/2.x/archaius2-archaius1-bridge)
so that archaius1 will be delegating to archaius2.

Note, if accessed via the static methods there is no guarantee the configuration will be setup
properly yet. If possible inject the `org.apache.commons.configuration.Configuration` to ensure
that setup is complete before use.

This is mostly for backwards compatibility when using libraries that have not migrated to
archaius 2.x yet.

## Gradle

```
compile "com.netflix.iep:iep-module-archaius1:${version_iep}"
```