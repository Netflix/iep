
## Description

Simple helper for working with guice and adding a listener for managing the life cycle. 

* Modules can be loaded via ServiceLoader to avoid repetition for simple modules.
* Supports life cycle using the [@PostConstruct](http://docs.oracle.com/javaee/7/api/javax/annotation/PostConstruct.html)
  and [@PreDestroy](http://docs.oracle.com/javaee/7/api/javax/annotation/PreDestroy.html)
  annotations.
  
Typical usage:

```java
public class Main {
  public static void main(String[] args) throws Exception {
    List<Module> modules = GuiceHelper.getModulesUsingServiceLoader();
    modules.add(new AbstractModule() {
      @Override public void configure() {
        // ... any final bindings ...
      }
    });
    GuiceHelper helper = new GuiceHelper();
    helper.start(modules);
    helper.addShutdownHook();
  }
}
```

## Gradle

```
compile "com.netflix.iep:iep-guice:${version_iep}"
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
