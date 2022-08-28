
## Description

Configures the app to use the SidecarRegistry implementation for the
[Spectator registry][registry].

> :warning: This will conflict with `netflix:atlas-client`. If running internally at Netflix,
> then the recommendation is to follow the [Netflix integration][netflix] guide instead. If
> you want to be an early adopter, then please sync up with the Insight team first.

[registry]: https://netflix.github.io/atlas-docs/spectator/lang/java/registry/overview/
[netflix]: https://netflix.github.io/atlas-docs/spectator/lang/java/usage/#netflix-integration

## Gradle

```
compile "com.netflix.iep:iep-spring-spectatord:${version_iep}"
```