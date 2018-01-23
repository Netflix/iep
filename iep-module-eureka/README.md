
## Description

Guice module to configure [eureka](https://github.com/Netflix/eureka). It will do the following:

* Setup binding so you can inject `ApplicationInfoManager` and `DiscoveryClient`.
* Configure the healthcheck handler to be based on
  [ServiceManager](https://github.com/Netflix/iep/tree/master/iep-service).

## Gradle

The `iep-module-archaius1` dependency sets up the bridge so that Eureka, which is built on
archaius1, can access the properties necessary to register and begin heartbeat.

```
compile "com.netflix.iep:iep-module-archaius1:${version_iep}"
compile "com.netflix.iep:iep-module-eureka:${version_iep}"
```

Internally, at Netflix, you will also need to pull in the config files from Artifactory:

```
compile "com.netflix.nfglue:nfglue-internal-configs:latest.release"
```