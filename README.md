# Insight Engineering Platform

Set of base libraries used primarily by the Insight Engineering team at Netflix to support
applications that need to run internally and externally.

## Overview

The internal platform libraries used at Netflix, provide many useful capabilities
that help make the applications more consistent, easier to debug, and generally
integrate well into the internal environment. For example it is straightforward
to:

* Examine the properties and jars being used on running instance.
* Get consistent logging and ability to adjust log levels dynamically for debugging.
* Configuring JMX so common JVM tooling can be used through the firewall.
* Register with [Eureka][eureka] and use it to communicate with other services.

Over time some of this functionality was extracted into standalone libraries, many of
which have been open sourced as [NetflixOSS Common Runtime Services and Libraries][netflixoss].
Examples are [Archaius][archaius] (configuration), [Eureka][eureka] (discovery service),
[Karyon][karyon] (base server), [Ribbon][ribbon] (Eureka aware HTTP client),
[Governator][governator] (dependency injection), and [blitz4j][blitz4j] (logging).

However, there are still some gaps and for many of the libraries mentioned above an internal
wrapper library needs to be used to work well internally. Originally the intent was for the
internal wrappers to get deprecated and phased out, but that turned out to be harder than
expected and never actually happened. Further some of these libraries like [Ribbon][ribbon]
and [blitz4j] are no longer receiving much investment. The IEP libraries were created as
part of the work to open source [Atlas][atlas] to allow us to:

* Have our open source applications be able to work the same way internally and externally.
* Ensure that the core debugging capabilities and key internal integrations work.
* Opt-in instead of opt-out. The internal platform provides a lot by default and you typically
  have to explicitly opt-out to turn off stuff you do not need. To make our apps lighter weight
  we wanted to explicitly opt-in instead. This has improved with some of the newer internal
  libraries, but is not available as part of [NetflixOSS][netflixoss] and thus doesn't satisfy
  our goal of consistency.

[netflixoss]: http://netflix.github.io/
[fit]: https://medium.com/netflix-techblog/fit-failure-injection-testing-35d8e2a9bb2
[atlas]: https://github.com/Netflix/atlas
[edda]: https://github.com/Netflix/edda
[archaius]: https://github.com/Netflix/archaius
[karyon]: https://github.com/Netflix/karyon
[ribbon]: https://github.com/Netflix/ribbon
[blitz4j]: https://github.com/Netflix/blitz4j
[eureka]: https://github.com/Netflix/eureka
[governator]: https://github.com/Netflix/governator

## Modules

Libraries named with a prefix of `iep-spring-` are Spring configurations that can be pulled in. All
Insight libraries should work using plain Spring, we do not rely on [Governator][governator]
extensions.

[PostConstruct]: http://docs.oracle.com/javaee/5/api/javax/annotation/PostConstruct.html
[PreDestroy]: http://docs.oracle.com/javaee/5/api/javax/annotation/PreDestroy.html

You can pick and choose just the set of modules you need. If one module requires another, then
it will install that module explicitly so you do not need to worry about the transitive
dependencies.

| Module                       | Description                                           |
|------------------------------|-------------------------------------------------------|
| [iep-spring-admin]           | Setup admin service for debugging the application.    |
| [iep-spring-atlas]           | Configure Spectator to use AtlasRegistry.             |
| [iep-spring-aws2]            | Setting up and injecting AWS clients.                 |
| [iep-spring-leader]          | Default bindings for the simple leader election API.  |
| [iep-spring-leader-dynamodb] | DynamoDB bindings for the simple leader election API. |
| [iep-spring-userservice]     | User service for validating known email addresses.    |


[iep-spring-admin]: https://github.com/Netflix/iep/tree/main/iep-spring-admin
[iep-spring-atlas]: https://github.com/Netflix/iep/tree/main/iep-spring-atlas
[iep-spring-aws2]: https://github.com/Netflix/iep/tree/main/iep-spring-aws2
[iep-spring-jmxport]: https://github.com/Netflix/iep/tree/main/iep-spring-jmxport
[iep-spring-leader]: https://github.com/Netflix/iep/tree/main/iep-spring-leader
[iep-spring-userservice]: https://github.com/Netflix/iep/tree/main/iep-spring-userservice

## Libraries

These are standalone libraries used in various Insight products.

| Module                   | Description                                                         |
|--------------------------|---------------------------------------------------------------------|
| [iep-admin]              | Simple admin service to aid in debugging.                           |
| [iep-leader-api]         | Simple leader election API with a default implementation.           |
| [iep-nflxenv]            | Configuration for accessing context from the environment.           |
| [iep-service]            | Simple abstraction for a service that is part of an application.    |
| [iep-ses]                | Helper for sending HTML emails with SES.                            |

[iep-admin]: https://github.com/Netflix/iep/tree/main/iep-admin
[iep-leader-api]: https://github.com/Netflix/iep/tree/main/iep-leader-api
[iep-nflxenv]: https://github.com/Netflix/iep/tree/main/iep-nflxenv
[iep-service]: https://github.com/Netflix/iep/tree/main/iep-service
[iep-ses]: https://github.com/Netflix/iep/tree/main/iep-ses

## Compatibility

The IEP libraries follow a semantic versioning scheme. Backwards incompatible changes
should be marked with an incremented major version number. Forwards compatibility
may work, but is in not required or guaranteed. It is highly recommended that all
`iep-*` versions in the classpath are the same.

Prior to 1.0, it was mostly backwards compatible with major changes resulting in the
minor version being incremented.

## Common Runtime Libraries Comparison

Where possible we will use the other [NetflixOSS Common Runtime Libraries][netflixoss], but
as the support for some of those libraries has waned the usage of several have been dropped.
This section will provide a quick summary of which parts we use and the differences.

### Archaius

Archaius is the primary configuration library used at Netflix. The primary feature over
other alternatives is that it can communicate with a property service to allow for properties
that can be changed at runtime. This can be used for things like feature flags to enable or
quickly disable functionality. Insight no longer uses [Archaius 2][a2].

It should be noted that as deployment automation and velocity has increased our (Insight team)
interest in runtime properties has waned. Changing runtime properties can be just as risky
as a deployment and adding proper checks such as canary analysis, staggered rollout, etc
to the property path means there is little advantage to using properties over just doing a
new deployment. The base layer for our configuration is using the [Typesafe Config][config]
library and many new uses inject that directly rather than injecting the Archaius2 Config
object.

[a2]: https://github.com/Netflix/archaius/tree/2.x
[config]: https://github.com/typesafehub/config

### Blitz4j

Logging library providing performance improvements on top of log4j 1.x and making the
logging levels configurable via properties. Most code should be using the slf4j interfaces
so the logging framework is pluggable, so this decision should only matter when selecting
a binding for running the application. For Insight apps we bind slf4j to vanilla log4j2 when
running internally. The logging configuration is mapped to a file that is monitored by
log4j2 so we can tune log levels on an instance.

### Eureka

Eureka is the Netflix service discovery system. All of the Insight apps should register
with Eureka to integrate well with internal systems that check this as part of ensuring
the service is healthy. Insight apps will map the [healthcheck endpoint][healthcheck] to
the service state so that healthcheck polling used to populate the Eureka state will accurately
reflect the service state.

[healthcheck]: https://github.com/Netflix/atlas/blob/master/atlas-akka/src/main/scala/com/netflix/atlas/akka/HealthcheckApi.scala

For client side uses, see section discussing [Ribbon](#ribbon).

### Governator

IEP is compatible with, but does not directly use or require [Governator][governator].
All Insight libraries should work using plain Spring, we do not rely on [Governator][governator].

### Karyon

Karyon provides the server framework and admin for internal applications. Internally it is
wrapped by the `base-server` library, though newer applications can use Karyon 2 directly.
For a long time we have only used the admin aspect of Karyon. The IEP admin is much lighter
weight and was originally developed as part of [Karyon 3][k3]. The Runtime team decided to
go a different direction so we simplified and inlined the aspects we care about as
[iep-admin].

[k3]: https://github.com/Netflix/karyon/tree/3.x

### Ribbon

Ribbon is the Eureka aware HTTP client layer used heavily at Netflix. Internally it is often
wrapped by the `platform-ipc` library and often referred to as NIWS (Netflix Integrated Web
Services). Similar to the story with [Karyon](#karyon), Insight was an early adopter of
[RxNetty] that was intended to become the underlying library powering Ribbon. Insight
developed the [iep-rxhttp] library to guinea pig RxNetty internally. It is still heavily
used by the Insight team for interacting with services that require middle tier load
balancing. However, the Runtime team has since de-prioritized [RxNetty] and is focusing
on [gRPC] for new use-cases. [Ribbon][ribbon] is not used by Insight and we'll likely phase
out [iep-rxhttp] over time.

Another trend is that since all Netflix services are now in the VPC, the AWS ELBs can
have proper security groups. That was not true in classic and was a big reason for initially
building out Eureka. Using ELBs and standard DNS means the selection of client library is
less important. The main add-on we want is consistent metrics and access logs which we
achieve by using the [Spectator HTTP log util][accesslog].

[gRPC]: http://www.grpc.io/
[RxNetty]: https://github.com/ReactiveX/RxNetty
[accesslog]: https://github.com/Netflix/spectator/blob/master/spectator-ext-sandbox/src/main/java/com/netflix/spectator/sandbox/HttpLogEntry.java

## Related Projects

Open source projects maintained by the Netflix Insight team are:

* Apps
    * [Atlas](https://github.com/Netflix/atlas): in-memory dimensional time series database.
    * [Edda](https://github.com/Netflix/edda): read-only cache of AWS resources.
    * [IEP Apps](https://github.com/Netflix-Skunkworks/iep-apps): small example apps using Insight libraries.
* Libraries
    * [AWSObjectMapper](https://github.com/Netflix/awsobjectmapper): provides mappings for Jackson 2
      to allow the AWS Java SDK model objects to easily be converted to/from JSON.
    * [Spectator](https://github.com/Netflix/spectator): client library for instrumenting an
      application to report data into Atlas or similar data stores.
* Legacy Libraries
    * [Edda Client](https://github.com/Netflix/edda-client): client library to access Edda via
      the AWS Java SDK interfaces.
    * [Rx AWS Java SDK](https://github.com/Netflix/rx-aws-java-sdk): experimental implementation
      of an AWS Java SDK that uses [RxNetty] internally and automatically handles pagination
      via RxJava Observables.
    * [Servo](https://github.com/Netflix/servo): legacy library for instrumenting code. Use
      [Spectator](https://github.com/Netflix/spectator) for new projects.
    

