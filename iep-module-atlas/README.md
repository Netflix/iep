
## Description

Configures the [Spectator registry][registry] to use the AtlasRegistry implementation. 

> :warning: This will conflict with `netflix:atlas-client`. If running internally at Netflix,
> then the recommendation is to follow the [Netflix integration][netflix] guide instead. If
> you want to be an early adopter, then please sync up with the Insight team first.

[registry]: http://netflix.github.io/spectator/en/latest/intro/registry/
[netflix]: http://netflix.github.io/spectator/en/latest/intro/netflix/

## Gradle

```
compile "com.netflix.iep:iep-module-atlas:${version_iep}"
```