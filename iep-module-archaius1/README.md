
## Description

Guice module to configure [archaius1](https://github.com/Netflix/archaius/tree/2.x). This pulls
in the 2.x archaius-legacy library and loads `application` as a cascaded property set. If you
want dynamic properties use 2.x, no remote configuration source will be configured.

This is mostly for backwards compatibility when using libraries that have not migrated to
archaius 2.x yet.

## Gradle

```
compile "com.netflix.iep:iep-module-archaius1:${version_iep}"
```