
## Description

Creates bindings for a factory to create AWS clients. The clients can be configured
so that the endpoint, client configuration, and credential provider can be tuned as
needed in the configuration file.

### Configuration

For more complete settings see the [reference.conf](https://github.com/Netflix/iep/blob/master/iep-module-aws/src/main/resources/reference.conf).
A quick example is shown below:

```hocon
netflix.iep.aws {
  // By default the name of the client is the service name as seen in the package.
  ec2 {
    // If not specified, then the local endpoint for the service will be chosen.
    endpoint = "foo.bar.com"
    
    // Client configuration settings can be set in the client block.
    client {
      connection-timeout = 5s
    }
    
    // Instead of using DefaultAWSCredentialsProviderChain do an assume role
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
// Add the module to the injector
Injector injector = Guice.createInjector(new AwsModule());

// Get factory instance and create a client
AwsClientFactory factory = injector.getInstance(AwsClientFactory.class);
AmazonEC2 dflt = factory.newInstance(AmazonEC2.class);
AmazonEC2 foo = factory.newInstance("foo-client", AmazonEC2.class);
```

### Injecting Client Interface

Sometimes it is more useful to directly inject the client interface rather than injecting the
`AwsClientFactory`. For example, the client interface is easier to mock out for unit tests.
This module does not create bindings for every client or have dependencies on all of the
sub-packages for the AWS SDK. However, it is straightforward to create a binding for the
interface yourself:

```java
// Add the module to the injector
Module module = new AbstractModule() {
  @Override protected void configure() {
  }
      
  @Provides
  private AmazonEC2 providesEC2(AwsClientFactory factory) {
    return factory.newInstance(AmazonEC2.class);
  }
};
Injector injector = Guice.createInjector(module, new AwsModule());

// Get instance
AmazonEC2 dflt = factory.newInstance(AmazonEC2.class);
```

## Gradle

```
compile "com.netflix.iep:iep-module-aws:${version_iep}"
```
