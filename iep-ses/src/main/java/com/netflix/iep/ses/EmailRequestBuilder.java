/*
 * Copyright 2014-2018 Netflix, Inc.
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

import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * <p>Helper for building {@link RawMessage} requests for the common case of an HTML or
 * test email message with some attachments. For more details on Amazon's recommendations
 * see <a href="http://docs.aws.amazon.com/ses/latest/DeveloperGuide/send-email-raw.html">sending
 * raw email</a>. With this class the usage is much simpler, e.g.:</p>
 *
 * <pre>
 * AmazonSimpleEmailService client = ...
 * client.sendRawEmail(new EmailRequestBuilder()
 *   .withSource("bob@example.com")
 *   .withToAddresses("andrew@example.com")
 *   .withSubject("Test message")
 *   .withHtmlBody("&lt;html&gt;&lt;body&gt;&lt;h1&gt;Alert!&lt;/h1&gt;&lt;p&gt;&lt;img src=\"cid:my-image.png\"&gt;&lt;/p&gt;&lt;/body&gt;&lt;/html&gt;")
 *   .addAttachment(Attachment.fromResource("image/png", "my-image.png"))
 *   .build();
 * );
 * </pre>
 */
public final class EmailRequestBuilder {

  private String source;
  private Destination destination;
  private List<String> replyToAddresses;
  private String subject;
  private String contentType;
  private String body;
  private List<Attachment> attachments;
  private String boundary;

  /** Create a new instance of the builder. */
  public EmailRequestBuilder() {
    destination = new Destination();
    replyToAddresses = Collections.emptyList();
    body = "";
    attachments = new ArrayList<>();
    boundary = UUID.randomUUID().toString();
  }

  /** For test cases we want a fixed boundary. */
  EmailRequestBuilder withBoundary(String boundary) {
    this.boundary = boundary;
    return this;
  }

  /** Set the source or from address of the message. This field is required. */
  public EmailRequestBuilder withSource(String source) {
    this.source = source;
    return this;
  }

  /** Set the destinations (to, cc, and bcc) for the message. */
  public EmailRequestBuilder withDestination(Destination destination) {
    this.destination = destination;
    return this;
  }

  /** Set the list of recipients for the message. */
  public EmailRequestBuilder withToAddresses(String... addresses) {
    destination.withToAddresses(addresses);
    return this;
  }

  /** Set the list of addresses to use for replies to the message. */
  public EmailRequestBuilder withReplyToAddresses(String... addresses) {
    this.replyToAddresses = Arrays.asList(addresses);
    return this;
  }

  /** Set the list of addresses to be copied on the message. */
  public EmailRequestBuilder withCcAddresses(String... addresses) {
    destination.withCcAddresses(addresses);
    return this;
  }

  /**
   * Set the list of addresses to be copied on the message without other recipients being
   * aware. This can be useful for privacy as well as minimizing spam for notification messages
   * if other recipients are likely to be uninterested in the replies (many have a habit of
   * reply all).
   */
  public EmailRequestBuilder withBccAddresses(String... addresses) {
    destination.withBccAddresses(addresses);
    return this;
  }

  /** Sets the main message. Note the charset will be ignored and UTF-8 will get used. */
  public EmailRequestBuilder withMessage(Message message) {
    withSubject(message.getSubject().getData());
    Body body = message.getBody();
    if (body.getHtml() != null) {
      withHtmlBody(body.getHtml().getData());
    } else if (body.getText() != null) {
      withTextBody(body.getText().getData());
    }
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
    return addresses.stream().collect(Collectors.joining(", "));
  }

  /**
   * Creates the raw email request for use with
   * {@link com.amazonaws.services.simpleemail.AmazonSimpleEmailService#sendRawEmail(SendRawEmailRequest)}.
   */
  public SendRawEmailRequest build() {
    return new SendRawEmailRequest().withRawMessage(toRawMessage());
  }

  /**
   * Creates the {@link RawMessage}. Can be used if additional modifications to the message
   * are needed before creating the request object.
   */
  public RawMessage toRawMessage() {
    ByteBuffer buf = ByteBuffer.wrap(toString().getBytes(StandardCharsets.UTF_8));
    return new RawMessage().withData(buf);
  }

  /** Generates the MIME encoded string for the message. */
  @Override public String toString() {
    final String mimeBoundary = "--" + boundary;
    StringBuilder builder = new StringBuilder();

    if (subject == null || subject.isEmpty()) {
      throw new IllegalArgumentException("subject not specified");
    }

    if (source == null || source.isEmpty()) {
      throw new IllegalArgumentException("no from address specified");
    }

    final String to = encodeAddressList(destination.getToAddresses());
    if (to == null || to.isEmpty()) {
      throw new IllegalArgumentException("no recipients specified");
    }

    builder
        .append(EmailHeader.mime())
        .append(EmailHeader.multipart(boundary))
        .append(EmailHeader.from(source))
        .append(EmailHeader.to(to))
        .append(EmailHeader.subject(subject));

    final String cc = encodeAddressList(destination.getCcAddresses());
    if (cc != null && !cc.isEmpty()) {
      builder.append(EmailHeader.cc(cc));
    }

    final String bcc = encodeAddressList(destination.getBccAddresses());
    if (bcc != null && !bcc.isEmpty()) {
      builder.append(EmailHeader.bcc(bcc));
    }

    final String replyTo = encodeAddressList(replyToAddresses);
    if (replyTo != null && !replyTo.isEmpty()) {
      builder.append(EmailHeader.replyTo(replyTo));
    }

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
