
## Description

Guice module to configure [archaius1](https://github.com/Netflix/archaius). This module will
setup the override layer (dynamic properties) to the properties found in the archaius2 config.
In other words, any property set via archaius2 will take precedence over archaius1 properties
except for properties set via the archaius1 runtime layer.

So properties will get chosen in the following order:

* 1.x runtime
* 1.x override
    * 2.x runtime
    * 2.x override (dynamic properties from platformservice)
    * 2.x system (System.getProperty)
    * 2.x environment (System.getenv)
    * 2.x application
    * 2.x library
* 1.x application (nothing loaded to this layer)
* 1.x library

Note, if accessed via the static methods there is no guarantee the configuration will be setup
properly yet. If possible inject the `AbstractConfiguration` to ensure that setup is complete
before use.

This is mostly for backwards compatibility when using libraries that have not migrated to
archaius 2.x yet.

## Gradle

```
compile "com.netflix.iep:iep-module-archaius1:${version_iep}"
```