
## Description

Guice module to configure [archaius2](https://github.com/Netflix/archaius/tree/2.x) for use
internally at Netflix. It will do the following:

* Setup binding so you can inject `AppConfig` and `Config`.
* Add typesafe config as an override layer.
* Add a dynamic layer that queries the internal platformservice for properties that can change
  at runtime.

It will also install the base guice module from archaius2. The `AppConfig` is customized to
use `application` as the config name rather than the default of `config`.
  
> :warning: This module will not load global properties. Properties coming from platformservice
> must be restricted to via at least one of application, cluster, or auto-scaling group.

## Gradle

```
compile "com.netflix.iep:iep-module-archaius2:${version_iep}"
```