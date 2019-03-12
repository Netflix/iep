
## Description

> :warning: **Deprecated:** This library depends on RxNetty which is unsupported and
> RxJava 1.x which reached [end of life] on March 31, 2018. Move to a supported HTTP client.

[end of life]: https://github.com/ReactiveX/RxJava/releases/tag/v1.3.8

Simple wrapper on top of [RxNetty](https://github.com/ReactiveX/RxNetty) that provides
integration with [Eureka](https://github.com/Netflix/eureka/) and
[Archaius2](https://github.com/Netflix/archaius/tree/2.x).

## Gradle

```
compile "com.netflix.iep:iep-rxhttp:${version_iep}"
```

## URI Schemes

RxHttp supports several schemes to help map a use case to the corresponding configuration
settings.

* `niws://{config}/{uri}`: make the request to `uri` using the configuration settings
  prefixed with `config`. If `uri` is relative, then then the set of servers will be
  found by querying Eureka for `${config}.niws.client.DeploymentContextBasedVipAddresses`.
* `vip://{config}:{vip}/{uri}`: make the request to `uri` using the configuration settings
  prefixed with `config` and looking up servers using the specified `vip`. This allows the
  same base config settings to be used with an arbitrary vip.

If using a normal scheme of `http` or `https`, then the config name of `default` will be
used.

## Configuration

All client properties have a name like:

```
{configName}.niws.client.{propertyName}
```

The supported property names are:

| **Property**                             | **Description**                                                   |
|------------------------------------------|-------------------------------------------------------------------|
| Port                                     | Server port number, defaults to (http:80 or https:443)            |
| ConnectTimeout                           | Network connect timeout in millis, default to 1s                  |
| ReadTimeout                              | Network read timeout for the socket in millis, default 30s        |
| ConnectionActiveLifeAge                  | Maximum age of connection in the pool in millis                   |
| FollowRedirects                          | Number of redirects client should automatically follow, default 3 |
| MaxConnectionsPerHost                    | Maximum number of connections to a specific host, default 20      |
| MaxConnectionsTotal                      | Maximum number of connections total, default 200                  |
| UseIpAddress                             | Should it use the IP from the Eureka metadata, default true       |
| GzipEnabled                              | Handle request/response compression automatically, default true   |
| WireLoggingEnabled                       | Enable netty wire logging, default false                          |
| WireLoggingLevel                         | Level to use for netty wire logging, default ERROR                |
| MaxAutoRetriesNextServer                 | Number of retries to a different server if possible, default 2    |
| RetryDelay                               | Initial delay between retries if throttled, default 500ms         |
| RetryReadTimeouts                        | Should it retry on read timeouts? Default true                    |
| UserAgent                                | User agent to use for the client, default RxHttp                  |
| DeploymentContextBasedVipAddresses       | Comma separated list of VIPs to use for querying Eureka           |
