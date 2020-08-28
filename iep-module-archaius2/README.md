
## Description

> :warning: **Deprecated:** For new work prefer [iep-module-dynconfig][dynconfig].

[dynconfig]: https://github.com/Netflix/iep/tree/master/iep-module-dynconfig

Guice module to configure [archaius2](https://github.com/Netflix/archaius/tree/2.x) for use
internally at Netflix. It will install the archaius2 guice module and put in the following
overrides:

* Add typesafe config as the application layer.
* Add an override layer that queries the internal platformservice for properties that can change
  at runtime.

> :warning: This module will not load global properties. Properties coming from platformservice
> must be restricted to at least one of application, cluster, or auto-scaling group.

## Gradle

```
compile "com.netflix.iep:iep-module-archaius2:${version_iep}"
```
