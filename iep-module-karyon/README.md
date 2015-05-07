
## Description

Guice module to configure the [karyon admin](https://github.com/Netflix/karyon). It primarily
just sets properties so that the paths for the UI and APIs match the old paths on `base-server`.
This means that internal tools like Asgard, Spinnaker, Client Deprecation Report, etc will
continue to function and do not need to know if you are on the legacy platform version or karyon.

## Gradle

```
compile "com.netflix.iep:iep-module-karyon:${version_iep}"
```

Karyon will load tabs based on classpath scanning. Some additional plugins you may want:

### Eureka Plugin

Show a summary of the eureka state.

```
compile 'com.netflix.karyon:karyon2-admin-eureka-plugin:2.7.0'
```
