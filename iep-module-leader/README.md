
## Description

This component provides a default service and injection module for leader election. By default, it
binds the `StandardLeaderElector` implementation for the `LeaderElector` and `LeaderStatus`
interfaces.

### Basic Usage

```java
public class YourAppService extends AbstractService {
  @Inject
  public YourAppService(LeaderStatus leaderStatus) {
    this.leaderStatus = leaderStatus;
  }

  private void runTask() {
    if(leaderStatus.hasLeadership()) {
      // do the task
    }
  }
}
```

For the common use case of a single leader in a cluster for a single resource, there is minimal
effort required to get to the above. See the `README.md` files in the other components for info on
additional features, such as leadership for multiple resources.

The steps are as follows:

1. Add a dependency to this module.
2. Add a dependency for a `LeaderDatabase` implementation, such as `iep-leader-dynamodb`.
3. `@Inject` a `LeaderStatus` into the component that needs to check leadership status (code above).
4. Start your service using `com.netflix.iep.guice.Main.run(args, new YourAppModule());`

### Metrics

#### leader.electionFailure

**Unit:** failures/second

**Dimensions:**

* Common Infrastructure

#### leader.timeSinceLastElection

**Unit:** seconds 

**Dimensions:**

* Common Infrastructure
