
## Description

Minimal utility to build a standalone jar file for a java application. The primary feature
[OneJAR][OneJar] has that this library does not is:

> No pollution: dependency jars are not expanded into the filesystem at runtime

This library basically creates a self-expanding zip file that will expand the jars to the
file system and then uses a [URLClassLoader][URLClassLoader]. For a number of our use-cases
OneJAR's custom class loader used to access jar files inside of the standalone jar caused
issues and prevented the application from running. The approach used here is really dead
simple and thus far we have not observed any compatibility issues with it. However, it does
come at the cost of some filesystem pollution.

[OneJAR]: http://one-jar.sourceforge.net/
[URLClassLoader]: https://docs.oracle.com/javase/8/docs/api/java/net/URLClassLoader.html

### Usage

To get the launcher:

```bash
$ LAUNCHER_JAR_URL="http://jcenter.bintray.com/com/netflix/iep/iep-launcher/${version_iep}/iep-launcher-${version_iep}.jar
$ curl -L $LAUNCHER_JAR_URL -o target/iep-launcher.jar
```

To build a standalone jar file:

```bash
$ java -classpath target/iep-launcher.jar com.netflix.iep.launcher.JarBuilder \
  		target/standalone.jar com.netflix.atlas.standalone.Main \
  		[jars ...]
```

Then to run the application:

```bash
$ java -jar target/standalone.jar
```

### Launcher Properties

The following system properties can be used to control the launcher bundled into the
standalone jar file:

| Property                             | Description                                                                               |
|--------------------------------------|-------------------------------------------------------------------------------------------|
| netflix.iep.launcher.mainClass       | Explicit main class instead of using the jar manifest.                                    |
| netflix.iep.launcher.workingDir      | Location to extract bundled jars. Default is `~/.iep-launcher/${standalone-jar-name}`     |
| netflix.iep.launcher.cleanWorkingDir | If true, then delete all files from working directory before extracting. Default is true. |
| netflix.iep.launcher.loggingEnabled  | Write out diagnostic information to stdout. Default is false.                             |

## Gradle

```
compile "com.netflix.iep:iep-launcher:${version_iep}"
```

