
## Description

Builder to make it easier to construct [SendRawEmailRequest] objects for the common case of
an HTML email message with a handful of attachments. For example, an alert email that provides
the [Atlas graph] indicating why the alert triggered as an image attachment.

[SendRawEmailRequest]: http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/simpleemail/model/SendRawEmailRequest.html
[Atlas graph]: https://github.com/Netflix/atlas/wiki

## Gradle

```
compile "com.netflix.iep:iep-ses:${version_iep}"
```

## Basic Usage

```java
// Construct the MIME encoded request
ByteBuffer data = new EmailRequestBuilder()
  .withFromAddress("bob@example.com")
  .withToAddresses("andrew@example.com")
  .withSubject("Test message")
  .withHtmlBody("<html><body><h1>Alert!</h1><p><img src=\"cid:graph.png\" /></p></body></html>")
  .addAttachment(Attachment.fromResource("image/png", "graph.png"))
  .toByteBuffer();

// If using AWS SDK for Java V1
AmazonSimpleEmailService client = new AmazonSimpleEmailServiceClient(
  new DefaultAWSCredentialsProviderChain());
RawMessage message = new RawMessage().withData(data);
client.sendRawEmail(new SendRawEmailRequest().withRawMessage(message));

// If using AWS SDK for Java V2
SesClient client = SesClient.create();
SendRawEmailRequest request = SendRawEmailRequest.builder()
  .rawMessage(RawMessage.builder().data(SdkBytes.fromByteBuffer(data)).build())
  .build();
client.sendRawEmail(request);
```

## Sending Authorization

To use with [sending authorization] there are two options. The first is to set the from address
and from identity ARN using the `EmailRequestBuilder`:

```java
// Construct the MIME encoded request
ByteBuffer data = new EmailRequestBuilder()
  .withFromArn("arn:aws:ses:us-east-1:123456789012:identity/example.com")
  .withFromAddress("bob@example.com")
  .withToAddresses("andrew@example.com")
  .withSubject("Test")
  .withTextBody("Body of the message.")
  .toByteBuffer();

// Send as in basic usage example
```

The other is to omit the from address when using `EmailRequestBuilder` and specify the source
and source arn on the `SendRawEmailRequest` object:

```java
// Construct the MIME encoded request, do not include from address
ByteBuffer data = new EmailRequestBuilder()
  .withToAddresses("andrew@example.com")
  .withSubject("Test")
  .withTextBody("Body of the message.")
  .toByteBuffer();

// If using AWS SDK for Java V1
AmazonSimpleEmailService client = new AmazonSimpleEmailServiceClient(
  new DefaultAWSCredentialsProviderChain());
SendRawEmailRequest request = new SendRawEmailRequest()
  .withSourceArn("arn:aws:ses:us-east-1:123456789012:identity/example.com")
  .withSource("bob@example.com")
  .withRawMessage(new RawMessage().withData(data));
client.sendRawEmail(request);

// If using AWS SDK for Java V2
SesClient client = SesClient.create();
SendRawEmailRequest request = SendRawEmailRequest.builder()
  .sourceArn("arn:aws:ses:us-east-1:123456789012:identity/example.com")
  .source("bob@example.com")
  .rawMessage(RawMessage.builder().data(SdkBytes.fromByteBuffer(data)).build())
  .build();
client.sendRawEmail(request);
```

[sending authorization]: https://docs.aws.amazon.com/ses/latest/DeveloperGuide/sending-authorization.html
