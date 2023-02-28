
## Description

Redis Cluster backed `LeaderDatabase` implementation.

### Usage
This `LeaderDatabase` implementation can be instantiated directly, injected programmatically, or
injected via Spring.
 
Once an instance is obtained, `initialize()` must be called. This is not called by default, since it
is a relatively heavy-weight, blocking operation, so is being left to the discretion of the user
of this API when to call it.

`RedisClusterLeaderDatabase` checks the connection to the Redis cluster via `initialize()` and on
return, the `LeaderDatabase` is ready for use.

### Configuration
By default, the leader ID is configured to the instance ID of the host and the resource IDs to the
single value of the cluster name. See the `reference.conf` file for other configuration options.
