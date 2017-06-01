
## Description

Provides a `eureka-client.properties` file suitable for running in test cases. That is it
will not attempt to connect out to a Eureka service for registration or fetching the registry.

## Gradle

```
compile "com.netflix.iep:iep-eureka-testconfig:${version_iep}"
```