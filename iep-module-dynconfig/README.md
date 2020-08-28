
## Description

Guice module that configures [iep-nflxenv] to refresh the dynamic override layer with
properties from [iep-archaius].

[iep-nflxenv]: https://github.com/Netflix/iep/blob/master/iep-nflxenv/README.md
[iep-archaius]: https://github.com/Netflix-Skunkworks/iep-apps/tree/master/iep-archaius

## Gradle

```
compile "com.netflix.iep:iep-module-dynconfig:${version_iep}"
```

## Usage

To leverage dynamic properties, inject the DynamicConfigManager and use it to work with
dynamic settings. Sample:

```java
public class Worker {
  @Inject
  public Worker(DynamicConfigManager manager) {
    // To get snapshot of the current full config with dynamic overrides
    Config config = manager.get();
    ... use config ...

    // To listen for changes to particular properties
    manager.addListener(ConfigListener.forBoolean("worker.enabled", this::enableToggled));
  }

  private void enableToggled(boolean enabled) {
    // This should be thread safe
    ... do something ...
  }
}
```

The DynamicConfigManager can also be accessed statically by calling
`ConfigManager.dynamicConfigManager()`, however, it is not recommended because there is no
guarantee the override layer will be loaded for the first time prior to use. This can cause
strange behavior on start-up if the remote properties are necessary to function properly.

## Config Precedence

The configuration will have the following layers:

* `ConfigManager.get()`: this is the base layer and will be similar to using the normal
  `ConfigFactory.load()` only with some care taken about the class loader that is used. It
  will also honor the `netflix.iep.include` setting to load some additional context specific
  config files.
* Remote Properties: the properties returned by the remote service other than
  `netflix.iep.override`. These are used for simple properties where the value can be encoded
  as a string.
* `netflix.iep.override`: special key in the remote properties where the value is an arbitrary
  config string. This allows for complex objects, lists, etc to be encoded and updated
  dynamically. Since it is a single value it is also preferred when multiple related settings
  are changed to ensure they are available atomically on the node.