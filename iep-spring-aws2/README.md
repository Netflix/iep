
## Description

Creates bindings for a factory to create clients using [AWS SDK for Java v2.0]. The clients can
be configured so that the region, basic client configuration, and credential provider can be
tuned as needed in the configuration file.

[AWS SDK for Java v2.0]: https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/welcome.html

### Configuration

For more complete settings see the [reference.conf]. A quick example is shown below:

[reference.conf]: https://github.com/Netflix/iep/blob/main/iep-spring-aws2/src/main/resources/reference.conf

```hocon
netflix.iep.aws {
  // By default the name of the client is the service name as seen in the package.
  ec2 {
    // If not specified, then the local endpoint for the service will be chosen.
    region = "us-east-1"

    // Client configuration settings can be set in the client block.
    client {
      api-call-attempt-timeout = 5s
    }

    // Instead of using DefaultCredentialsProvider do an assume role
    credentials {
      role-arn = "arn:aws:iam::1234567890:role/IepTest"
      role-session-name = "foo"
    }
  }

  // You can also use a custom name and reference it explicitly
  foo-client {
    // ...
  }
}
```

### Typical Usage

```java
// Create the Spring ApplicationContext, ensure com.netflix.iep.aws2.AwsConfiguration is included
ApplicationContext context = ...

// Get factory instance and create a client
AwsClientFactory factory = context.getBean(AwsClientFactory.class);
Ec2Client dflt = factory.newInstance(Ec2Client.class);
Ec2Client foo = factory.newInstance("foo-client", Ec2Client.class);

// Works with sync or async clients
Ec2AsyncClient dflt = factory.newInstance(Ec2AsyncClient.class);
Ec2AsyncClient foo = factory.newInstance("foo-client", Ec2AsyncClient.class);
```

### Injecting Client Interface

Sometimes it is more useful to directly inject the client interface rather than injecting the
`AwsClientFactory`. For example, the client interface is easier to mock out for unit tests.
This module does not create bindings for every client or have dependencies on all of the
sub-packages for the AWS SDK. However, it is straightforward to create a binding for the
interface yourself:

```java
@Configuration
public class AwsClientConfiguration {
  @Bean
  Ec2Client providesEC2(AwsClientFactory factory) {
    return factory.newInstance(Ec2Client.class);
  }
}
```

### Multi-Account Usage

When building an application that needs to communicate with many accounts, the factory can be
used to assume role into a specified account. Use a place holder in the role-arn setting:

```hocon
netflix.iep.aws {
  default {
    credentials {
      // The account placeholder will get replaced by the requested account id
      role-arn = "arn:aws:iam::{account}:role/IepTest"
      role-session-name = "foo"
    }
  }
}
```

Then when creating a new client pass in the account id, for example:

```java
AwsClientFactory factory = context.getBean(AwsClientFactory.class);
Ec2Client client1 = factory.newInstance(Ec2Client.class, "12345");
Ec2Client client2 = factory.newInstance(Ec2Client.class, "54321");
```

The `newInstance` call will always create a new instance of the client. To reuse a shared client
for a given account use `getInstance`:

```java
AwsClientFactory factory = context.getBean(AwsClientFactory.class);
Ec2Client client1 = factory.getInstance(Ec2Client.class, "12345");
Ec2Client client2 = factory.getInstance(Ec2Client.class, "54321");

// This will be the same instance as client1
Ec2Client client3 = factory.getInstance(Ec2Client.class, "12345");
```

## Gradle

```
compile "com.netflix.iep:iep-spring-aws2:${version_iep}"
```
