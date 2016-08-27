
## Description

Builder to make it easier to construct
[SendRawEmailRequest](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/simpleemail/model/SendRawEmailRequest.html)
objects for the common case of an HTML email message with a handful of attachments.
For example, an alert email that provides the [Atlas graph](https://github.com/Netflix/atlas/wiki)
indicating why the alert triggered as an image attachment.

Sample usage:

```java
AmazonSimpleEmailService client = new AmazonSimpleEmailServiceClient(
  new DefaultAWSCredentialsProviderChain());
client.sendRawEmail(new EmailRequestBuilder()
  .withSource("bob@example.com")
  .withToAddresses("andrew@example.com")
  .withSubject("Test message")
  .withHtmlBody("<html><body><h1>Alert!</h1><p><img src=\"cid:graph.png\" /></p></body></html>")
  .addAttachment(Attachment.fromResource("image/png", "graph.png"))
  .build());
```

## Gradle

```
compile "com.netflix.iep:iep-ses:${version_iep}"
```
