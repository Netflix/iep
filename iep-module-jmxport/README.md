
## Description

Guice module to pin the JMX port so it is easier to create an opening in the firewall. By default
it will setup JMX to listen for port 7500. For accessing JMX using tools like visualvm at Netflix
see [tunnel-helper](http://go/jmx).

The host and port can be set using the system properties:

* `netflix.iep.jmx.host`
* `netflix.iep.jmx.port`

## Gradle

```
compile "com.netflix.iep:iep-module-jmxport:${version_iep}"
```