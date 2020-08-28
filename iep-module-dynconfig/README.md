
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