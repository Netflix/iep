
## Description

Guice module to configure [eureka](https://github.com/Netflix/eureka). It will do the following:

* Setup binding so you can inject `ApplicationInfoManager` and `DiscoveryClient`.
* Configure the healthcheck handler to be based on
  [ServiceManager](https://github.com/Netflix/iep/tree/master/iep-service).

## Gradle

```
compile "com.netflix.iep:iep-module-eureka:${version_iep}"
```

Internally at Netflix you will also need to pull in the config files from artifactory:

```
compile "com.netflix.nfglue:nfglue-internal-configs:latest.release"
```