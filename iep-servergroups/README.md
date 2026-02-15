
## Description

Library for getting a view of the current set of server groups. This will merge views
from both [Edda] and [Eureka] to create the list.

[Edda] provides a cache of state from AWS and [Titus]. [Eureka] provides a view of
health based on whether the service is able to heartbeat successfully. In addition,
since [Eureka] is based on a push from the instances, it can be used as a sanity
check in case [Edda] is stale and does not know about new instances. However,
terminated instances would not be detected until [Edda] is up to date. The instances
will be treated as not registered and assumed to be failing to heartbeat if only
present in [Edda] and not [Eureka].

[Edda]: https://github.com/Netflix/edda
[Eureka]: https://github.com/Netflix/eureka
[Titus]: https://github.com/Netflix/titus

## Gradle

```
compile "com.netflix.iep:iep-servergroups:${version_iep}"
```

Internally, at Netflix, you will also need to pull in the config files from Artifactory:

```
compile "com.netflix.nfglue:nfglue-iep-configs:latest.release"
```

