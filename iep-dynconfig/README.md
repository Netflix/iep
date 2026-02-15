
## Description

Configuration for detecting information about the environment where the application is running.
The environment is derived from the standard environment variables used within the base AMI at
Netflix. Where possible a sensible default will be used to make running locally easier.

All of these settings can be accessed via the config using a prefix of `netflix.iep.env`. See
the [reference.conf][ref] for a complete listing.

[ref]: https://github.com/Netflix/iep/blob/master/iep-dynconfig/src/main/resources/reference.conf

### EC2 Settings

These are settings that are typically derived from the [EC2 instance metadata][meta] and
exposed as environment variables by the standard Netflix profile. Some of the most commonly
used settings are:

[meta]: http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html

| Name         | Environment Variable    | Description                       |
|--------------|-------------------------|-----------------------------------|
| ami          | EC2_AMI_ID              | [AWS Image ID][ami]               |
| instance-id  | NETFLIX_INSTANCE_ID     | AWS Instance ID                   |
| region       | EC2_REGION              | [AWS Region][regions]             |
| vmtype       | EC2_INSTANCE_TYPE       | [AWS instance type][vmtype]       |
| zone         | EC2_AVAILABILITY_ZONE   | [AWS Availability Zone][regions]  |

[ami]: http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/AMIs.html
[vmtype]: http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/instance-types.html
[regions]: http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/using-regions-availability-zones.html

### Environment Settings

These are settings that typically get set by Netflix when the system is configured. When
running in EC2 they are set via the user data in the launch config. All of these environment
variables will have a prefix of `NETFLIX_`.

| Name         | Environment Variable    | Description                                                                                     |
|--------------|-------------------------|-------------------------------------------------------------------------------------------------|
| account      | ACCOUNT                 | Account name, typically `$type$env`                                                             |
| account-env  | ACCOUNT_ENV             | Should always be test or prod                                                                   |
| account-type | ACCOUNT_TYPE            | Type of the account, usually matches the stack name of supporting services                      |
| account-id   | ACCOUNT_ID              | Id for the account                                                                              |
| environment  | ENVIRONMENT             | With new user data should always be test or prod. Older user data it may be the same as ACCOUNT |
| domain       | DOMAIN                  | DNS sub domain for this environment. Used to substitute config settings with host names         |

### Server Group Settings

Server group details. These follow the naming conventions defined by [frigga][frigga]. A
quick summary of the naming convention for server groups is showing below:

```
       cluster
╭─────────┴──────────╮
foo_webapp-main-canary-v042
╰───┬────╯ ╰┬─╯ ╰─┬──╯ ╰┬─╯
   app   stack  detail  sequence
```

[frigga]: https://github.com/Netflix/frigga

All of these environment variables will have a prefix of `NETFLIX_`. The property
names and environment variables are:

| Name         | Environment Variable    | Description                             |
|--------------|-------------------------|-----------------------------------------|
| app          | APP                     | Name of a particular software package   |
| cluster      | CLUSTER                 | A specific deployment of an application |
| asg          | AUTO_SCALE_GROUP        | Group of instances representing a service. In AWS this is an [auto-scaling group][asg]. |
| stack        | STACK                   | Used to associate across applications   |

[asg]: http://docs.aws.amazon.com/autoscaling/latest/userguide/AutoScalingGroup.html

## Gradle

```
compile "com.netflix.iep:iep-dynconfig:${version_iep}"
```