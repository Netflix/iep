
## Description

Guice module to configure the [karyon admin](https://github.com/Netflix/karyon/tree/3.x). It
starts up the admin and provide backwards compatible endpoints for appinfo, env, jars, and props.
This means that internal tools like Asgard, Spinnaker, Client Deprecation Report, etc will
continue to function and do not need to know if you are on the legacy platform version or karyon.

## Gradle

```
compile "com.netflix.iep:iep-module-karyon3:${version_iep}"
```

