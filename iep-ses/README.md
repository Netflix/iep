
## Description

Builder to make it easier to construct [SendRawEmailRequest] objects for the common case of
an HTML email message with a handful of attachments. For example, an alert email that provides
the [Atlas graph] indicating why the alert triggered as an image attachment.

[SendRawEmailRequest]: http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/simpleemail/model/SendRawEmailRequest.html
[Atlas graph]: https://github.com/Netflix/atlas/wiki

Sample usage:

```java
AmazonSimpleEmailService client = new AmazonSimpleEmailServiceClient(
  new DefaultAWSCredentialsProviderChain());
client.sendRawEmail(new EmailRequestBuilder()
  .withFromAddress("bob@example.com")
  .withToAddresses("andrew@example.com")
  .withSubject("Test message")
  .withHtmlBody("<html><body><h1>Alert!</h1><p><img src=\"cid:graph.png\" /></p></body></html>")
  .addAttachment(Attachment.fromResource("image/png", "graph.png"))
  .build());
```

## Sending Authorization

To use with [sending authorization] there are two options. The first is to set the from address
and from identity ARN using the `EmailRequestBuilder`:

```java
AmazonSimpleEmailService client = new AmazonSimpleEmailServiceClient(
  new DefaultAWSCredentialsProviderChain());
client.sendRawEmail(new EmailRequestBuilder()
  .withFromArn("arn:aws:ses:us-east-1:123456789012:identity/example.com")
  .withFromAddress("bob@example.com")
  .withToAddresses("andrew@example.com")
  .withSubject("Test")
  .withTextBody("Body of the message.")
  .build());
```

The other is to omit the from address when using `EmailRequestBuilder` and specify the source
and source arn on the `SendRawEmailRequest` object:

```java
AmazonSimpleEmailService client = new AmazonSimpleEmailServiceClient(
  new DefaultAWSCredentialsProviderChain());
RawMessage msg = new EmailRequestBuilder()
  .withToAddresses("andrew@example.com")
  .withSubject("Test")
  .withTextBody("Body of the message.")
  .toRawMessage();
SendRawEmailRequest request = new SendRawEmailRequest()
  .withSourceArn("arn:aws:ses:us-east-1:123456789012:identity/example.com")
  .withSource("bob@example.com")
  .withRawMessage(msg);
client.sendRawEmail(request);
```

[sending authorization]: https://docs.aws.amazon.com/ses/latest/DeveloperGuide/sending-authorization.html

## Gradle

```
compile "com.netflix.iep:iep-ses:${version_iep}"
```
