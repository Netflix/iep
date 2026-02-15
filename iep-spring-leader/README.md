
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
    if (leaderStatus.hasLeadership()) {
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

#### leader.elections

**Unit:** elections/second

**Dimensions:**

* `result`: One of `success` or `failure`.
* `error`: The error that occurred, if `result` is `failure`.
* Common Infrastructure

#### leader.electorInitializeDuration

The total time taken for the leader elector to complete initialization.

**Unit:** seconds

**Dimensions:**

* Common Infrastructure

#### leader.timeSinceLastElection

The time since the last successfully completed election.

**Unit:** seconds 

**Dimensions:**

* Common Infrastructure
