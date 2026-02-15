
## Description

Simple admin service for debugging the application. Typically this will listen on port 8077
and provide a set of endpoints with useful metadata about the application. For some of the key
endpoints it is compatible with the [Karyon admin] output so internal Netflix tooling that
relies on those endpoints will still work.

[karyon]: https://github.com/Netflix/karyon

### Default Endpoints

| Path            | Description                                             |
|-----------------|---------------------------------------------------------|
| /env            | Environment variables for the JVM process.              |
| /eureka         | Dump the data in the local eureka cache.                |
| /guice          | Debug information from the guice injector.              |
| /jars           | List of jars that are in the classpath.                 |
| /jmx            | Dump of JMX mbeans.                                     |
| /props          | Properties visible via [archaius2][archaius].           | 
| /resources      | List the available endpoints.                           | 
| /services       | List of [services][service] and their state.            |
| /spectator      | List of metrics registered with [Spectator][spectator]. |
| /system         | System properties for the JVM.                          |
| /threads        | List of all threads and their stack traces.             |

[service]: https://github.com/Netflix/iep/tree/master/iep-service
[archaius]: https://github.com/Netflix/iep/tree/master/iep-module-archaius2
[spectator]: http://netflix.github.io/spectator/en/latest/

### Custom Endpoints

A custom endpoint is created by implementing the [HttpEndpoint][HttpEndpoint] interface. If
using guice you can then register by using the [AdminModule][module-admin] helper. For example:

```java
public class MyModule extends AbstractModule {
  @Override protected void configure() {
    AdminModule.endpointBinder(binder())
      .addBinding("/foo").to(FooEndpoint.class);
  }
}
```

[HttpEndpoint]: https://github.com/Netflix/iep/blob/master/iep-admin/src/main/java/com/netflix/iep/admin/HttpEndpoint.java
[module-admin]: https://github.com/Netflix/iep/tree/master/iep-module-admin

An endpoint can also use duck typing. That is it can implement the methods of
[HttpEndpoint][HttpEndpoint] without actually implementing the interface and still work in
the binding. The main use-case for this is providing an endpoint class for libraries where
they need to have minimal dependencies and cannot depend on the admin library. The admin
library would not be required, but if it is available for an application and they setup a
binding then they can get the endpoint.

## Gradle

```
compile "com.netflix.iep:iep-admin:${version_iep}"
```