
## Description

DynamoDB backed `LeaderDatabase` implementation.

### Usage
This `LeaderDatabase` implementation can be instantiated directly, injected programmatically, or
injected. For convenience, a Guice module is provided that is configured for discovery by
 `java.util.ServiceLoader`.
 
Once an instance is obtained, `initialize()` must be called. This is not called by default, since it
is a relatively heavy-weight, blocking operation, so is being left to the discretion of the user
of this API when to call it.

`DynamoDbLeaderDatabase` creates database table and waits for the active status in `initialize()`
so, once `initialize()` returns, the `LeaderDatabase` is ready for use.

### Configuration
By default, the leader ID is configured to the instance ID of the host and the resource IDs to the
single value of the cluster name. See the `reference.conf` file for other configuration options.
