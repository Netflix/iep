
## Description

Simple helper for working with Spring. Typical usage:

```java
public class Main {
  public static void main(String[] args) throws Exception {
    com.netflix.iep.spring.Main.run(args);
  }
}
```

## Gradle

```
compile "com.netflix.iep:iep-spring:${version_iep}"
```

## Shutdown

If using the normal shutdown helper it will register a
[shutdown hook](https://docs.oracle.com/javase/8/docs/api/java/lang/Runtime.html#addShutdownHook-java.lang.Thread-)
with the JVM. Be aware of other services that may be using shutdown hooks.

One common example is logging. Log4j2 also uses a
[shutdown hook by default](https://logging.apache.org/log4j/2.x/log4j-core/apidocs/org/apache/logging/log4j/core/util/DefaultShutdownCallbackRegistry.html).
This can be disabled in the log4j [configuration](https://logging.apache.org/log4j/2.x/manual/configuration.html#ConfigurationSyntax),
for example:

```xml
<Configuration status="warn" shutdownHook="disable">
...
</Configuration>
```
