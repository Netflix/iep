
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
    List<Module> modules = Governator.getModulesUsingServiceLoader();
    modules.add(new AbstractModule() {
      @Override public void configure() {
        // ... any final bindings ...
      }
    });
    Governator gov = new Governator();
    gov.start(modules);
    gov.addShutdownHook();
  }
}
```

## Gradle

```
compile "com.netflix.iep:iep-guice:${version_iep}"
```