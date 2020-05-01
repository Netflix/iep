/*
 * Copyright 2014-2020 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.iep.ses;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * <p>Helper for building {@code RawMessage} requests for the common case of an HTML or
 * test email message with some attachments. For more details on Amazon's recommendations
 * see <a href="http://docs.aws.amazon.com/ses/latest/DeveloperGuide/send-email-raw.html">sending
 * raw email</a>. With this class the usage is much simpler, e.g. with the v1 SDK:</p>
 *
 * <pre>
 * AmazonSimpleEmailService client = ...
 * RawMessage message = new RawMessage().withData(
 *   new EmailRequestBuilder()
 *     .withFromAddress("bob@example.com")
 *     .withToAddresses("andrew@example.com")
 *     .withSubject("Test message")
 *     .withHtmlBody("&lt;html&gt;&lt;body&gt;&lt;h1&gt;Alert!&lt;/h1&gt;&lt;p&gt;&lt;img src=\"cid:my-image.png\"&gt;&lt;/p&gt;&lt;/body&gt;&lt;/html&gt;")
 *     .addAttachment(Attachment.fromResource("image/png", "my-image.png"))
 *     .toByteBuffer()
 * );
 * client.sendRawEmail(new SendRawEmailRequest().withRawMessage(message));
 * </pre>
 *
 * <p>With the v2 SDK:</p>
 *
 * <pre>
 * SesClient client = SesClient.create();
 * SdkBytes data = SdkBytes.fromByteBuffer(
 *   new EmailRequestBuilder()
 *     .withFromAddress("bob@example.com")
 *     .withToAddresses("andrew@example.com")
 *     .withSubject("Test message")
 *     .withHtmlBody("&lt;html&gt;&lt;body&gt;&lt;h1&gt;Alert!&lt;/h1&gt;&lt;p&gt;&lt;img src=\"cid:my-image.png\"&gt;&lt;/p&gt;&lt;/body&gt;&lt;/html&gt;")
 *     .addAttachment(Attachment.fromResource("image/png", "my-image.png"))
 *     .toByteBuffer()
 * );
 * SendRawEmailRequest request = SendRawEmailRequest.builder()
 *   .rawMessage(RawMessage.builder().data(data).build())
 *   .build();
 * client.sendRawEmail(request);
 * </pre>
 */
public final class EmailRequestBuilder {

  private String fromAddress;
  private String fromArn;
  private List<String> toAddresses;
  private List<String> ccAddresses;
  private List<String> bccAddresses;
  private List<String> replyToAddresses;
  private Map<String, String> headers;
  private String configSet;
  private String subject;
  private String contentType;
  private String body;
  private List<Attachment> attachments;
  private String boundary;

  /** Create a new instance of the builder. */
  public EmailRequestBuilder() {
    toAddresses = Collections.emptyList();
    ccAddresses = Collections.emptyList();
    bccAddresses = Collections.emptyList();
    replyToAddresses = Collections.emptyList();
    headers = new LinkedHashMap<>();
    body = "";
    attachments = new ArrayList<>();
    boundary = UUID.randomUUID().toString();
  }

  /** For test cases we want a fixed boundary. */
  EmailRequestBuilder withBoundary(String boundary) {
    this.boundary = boundary;
    return this;
  }

  /**
   * Set the source or from address of the message. If not specified, then it must be
   * provided when constructing the {@code SendRawEmailRequest} object.
   */
  public EmailRequestBuilder withFromAddress(String address) {
    this.fromAddress = address;
    return this;
  }

  /**
   * Set the ARN of the identity that is associated with the sending authorization policy
   * that permits you to specify a particular "From" address in the header of the raw email.
   * The ARN will be encoded in the message with the {@code X-SES-FROM-ARN} header.
   */
  public EmailRequestBuilder withFromArn(String fromArn) {
    this.fromArn = fromArn;
    return this;
  }

  /** Set the list of recipients for the message. */
  public EmailRequestBuilder withToAddresses(String... addresses) {
    this.toAddresses = Arrays.asList(addresses);
    return this;
  }

  /** Set the list of addresses to use for replies to the message. */
  public EmailRequestBuilder withReplyToAddresses(String... addresses) {
    this.replyToAddresses = Arrays.asList(addresses);
    return this;
  }

  /** Set the list of addresses to be copied on the message. */
  public EmailRequestBuilder withCcAddresses(String... addresses) {
    this.ccAddresses = Arrays.asList(addresses);
    return this;
  }

  /**
   * Set the list of addresses to be copied on the message without other recipients being
   * aware. This can be useful for privacy as well as minimizing spam for notification messages
   * if other recipients are likely to be uninterested in the replies (many have a habit of
   * reply all).
   */
  public EmailRequestBuilder withBccAddresses(String... addresses) {
    this.bccAddresses = Arrays.asList(addresses);
    return this;
  }

  /**
   * Add a custom header to the email message.
   */
  public EmailRequestBuilder addHeader(String key, String value) {
    EmailHeader.checkCustomHeader(key);
    this.headers.put(key, value);
    return this;
  }

  /**
   * Specifies an SES configuration set to use for the message.
   *
   * @see <a href="https://docs.aws.amazon.com/ses/latest/DeveloperGuide/using-configuration-sets.html">configuration sets</a>
   */
  public EmailRequestBuilder withConfigSet(String configSet) {
    this.configSet = configSet;
    return this;
  }

  /** Sets the subject of the message. This field is required. */
  public EmailRequestBuilder withSubject(String subject) {
    this.subject = subject;
    return this;
  }

  /** Sets the body of the message using a content type of {@code text/html}. */
  public EmailRequestBuilder withHtmlBody(String body) {
    this.contentType = "text/html; charset=UTF-8";
    this.body = body;
    return this;
  }

  /** Sets the body of the message using a content type of {@code text/plain}. */
  public EmailRequestBuilder withTextBody(String body) {
    this.contentType = "text/plain; charset=UTF-8";
    this.body = body;
    return this;
  }

  /** Adds an attachment to the message. */
  public EmailRequestBuilder addAttachment(Attachment attachment) {
    attachments.add(attachment);
    return this;
  }

  private String encodeAddressList(List<String> addresses) {
    return String.join(", ", addresses);
  }

  /**
   * Creates a {@link ByteBuffer} containing the MIME encoded raw message for the email.
   */
  public ByteBuffer toByteBuffer() {
    return ByteBuffer.wrap(toByteArray());
  }

  /**
   * Creates a byte array containing the MIME encoded raw message for the email.
   */
  public byte[] toByteArray() {
    return toString().getBytes(StandardCharsets.UTF_8);
  }

  /** Generates the MIME encoded string for the message. */
  @Override public String toString() {
    final String mimeBoundary = "--" + boundary;
    StringBuilder builder = new StringBuilder();

    if (subject == null || subject.isEmpty()) {
      throw new IllegalArgumentException("subject not specified");
    }

    // Can be specified by calling withSource on the SendRawEmailRequest object instead. That
    // is necessary if you want to use withSourceArn for sending authorization. If the source
    // is provided here, then withFromArn must be used instead.
    final String fromHeader = (fromAddress == null || fromAddress.isEmpty())
        ? ""
        : EmailHeader.from(fromAddress).toString();

    final String to = encodeAddressList(toAddresses);
    if (to.isEmpty()) {
      throw new IllegalArgumentException("no recipients specified");
    }

    builder
        .append(EmailHeader.mime())
        .append(EmailHeader.multipart(boundary))
        .append(fromHeader)
        .append(EmailHeader.to(to))
        .append(EmailHeader.subject(subject));

    final String cc = encodeAddressList(ccAddresses);
    if (!cc.isEmpty()) {
      builder.append(EmailHeader.cc(cc));
    }

    final String bcc = encodeAddressList(bccAddresses);
    if (!bcc.isEmpty()) {
      builder.append(EmailHeader.bcc(bcc));
    }

    final String replyTo = encodeAddressList(replyToAddresses);
    if (!replyTo.isEmpty()) {
      builder.append(EmailHeader.replyTo(replyTo));
    }

    if (fromArn != null && !fromArn.isEmpty()) {
      builder.append(EmailHeader.fromArn(fromArn));
    }

    if (configSet != null && !configSet.isEmpty()) {
      builder.append(EmailHeader.configSet(configSet));
    }

    headers.forEach((k, v) -> builder.append(EmailHeader.custom(k, v)));

    builder
        .append(EncodingUtils.CRLF)
        .append(mimeBoundary).append(EncodingUtils.CRLF)
        .append(EmailHeader.contentType(contentType))
        .append(EmailHeader.contentTransferEncoding("base64"))
        .append(EncodingUtils.CRLF)
        .append(EncodingUtils.base64(body.getBytes(StandardCharsets.UTF_8)))
        .append(EncodingUtils.CRLF);

    for (Attachment attachment : attachments) {
      builder.append(mimeBoundary).append(EncodingUtils.CRLF)
          .append(attachment.toString());
    }

    builder.append(mimeBoundary).append("--").append(EncodingUtils.CRLF);
    return builder.toString();
  }
}
