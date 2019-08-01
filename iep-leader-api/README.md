
## Description

Simple leader election API for cases that a single entity within a group should complete tasks for a
set of _resources_. _Resource_ in this context is defined by an entity that can be identified by a
Unique ID. _Sharding_ is not currently supported natively in this API. However, one can select
resources using a sharding algorithm externally and then use this API for leadership management.

Supporting correctness guarantees is not specified by the API and up to the implementation. See
[How to do distributed locking][kleppmann], for a detailed exploration of distributed locking in
support of correctness vs efficiency.

[kleppmann]: https://martin.kleppmann.com/2016/02/08/how-to-do-distributed-locking.html

### Usage

Typical usage consists of binding `LeaderElector` and `LeaderStatus` to `StandardLeaderElector`
using an inversion of control framework, such as Guice or Spring Injection. One can also instantiate
`StandardLeaderElector` directly. `initialize()` should be called before any other methods. After
that, the other methods may be interleaved in any fashion. `runElection()` should be called
periodically based upon the value of `iep.leader.leaderElectorFrequency` which, in turn, should be
set to something reasonable based upon `iep.leader.maxIdleDuration`.

### Configuration

A value for the `iep.leader.leaderId` property must be set.

If the resources comprise a constant list, optionally set the property `iep.leader.resourceIds`.
Resources can be added with either of, or combination of, that property and programmatically with
the `LeaderElector.addResource()` API.

NOTE: The default `LeaderDatabase` implementation in `iep-leader-dynamodb` sets the leader ID to the
instance ID and the resource IDs to the single value of the cluster name as the default.

Additional configuration options are documented inline in `reference.conf`.

### Metrics

#### leader.removals

**Unit:** removals/second

**Dimensions:**

* `resource`: The ID of the resource for which removal of the leader failed.
* `result`: One of `success` or `failure`.
* `error`: The error that occurred, if `result` is `failure`.
* Common Infrastructure

#### leader.resourceLeader

**Unit:** boolean - 1.0 if reporting instance is leader of `resource`, 0.0 if not

**Dimensions:**

* `resource`: The ID of the resource.
* `leader`: The leader ID.
* Common Infrastructure

#### leader.resourceWithNoLeader

**Unit:** boolean - 1.0 if the leader of `resource` is specifically _NO_LEADER_, 0.0 otherwise

**Dimensions:**

* `resource`: The ID of the resource.
* Common Infrastructure
