/*
 * Copyright 2014-2023 Netflix, Inc.
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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RunWith(JUnit4.class)
public class EmailRequestBuilderTest {

  private static final boolean BLESS = false;
  private static final boolean SEND = false;

  private static final String FROM = "bob@example.com";
  private static final String TO = "andrew@example.com";
  private static final String CC = "sue@example.com";

  private static final String FROM_ARN = "arn:aws:ses:us-east-1:123456789012:identity/example.com";

  private static final String BOUNDARY = "331239ab-8a31-4cc6-84d6-5557f96ebc3a";

  private void writeResource(String name, byte[] data) throws IOException {
    // Path ends with: iep-ses/target/test-classes/des-example.png
    URL url = Thread.currentThread().getContextClassLoader().getResource("des-example.png");
    Path projectDir = Paths.get(url.getPath()).getParent().getParent().getParent();
    Path resourceDir = projectDir.resolve("src/test/resources");

    // Sanity check that resource dir exists
    if (!Files.isDirectory(resourceDir)) {
      throw new IOException("cannot find resource directory: " + resourceDir);
    }

    // Write data to file
    try (OutputStream out = Files.newOutputStream(resourceDir.resolve(name))) {
      out.write(data);
    }
  }

  private String readResource(String name) throws IOException {
    // Path ends with: iep-ses/target/test-classes/des-example.png
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    try (InputStream in = cl.getResourceAsStream(name)) {
      return new String(EncodingUtils.readAll(in), StandardCharsets.US_ASCII);
    }
  }

  private void checkMessage(String name, EmailRequestBuilder builder) throws IOException {
    builder.withBoundary(BOUNDARY);
    if (BLESS) {
      String message = builder.toString();
      writeResource(name, message.getBytes(StandardCharsets.US_ASCII));
    } else if (SEND) {
      try (SesClient client = SesClient
          .builder()
          .credentialsProvider(DefaultCredentialsProvider.create())
          .region(Region.US_EAST_1)
          .build()) {
        SendRawEmailRequest request = SendRawEmailRequest
            .builder()
            .rawMessage(RawMessage
                .builder()
                .data(SdkBytes.fromByteBuffer(builder.toByteBuffer()))
                .build()
            )
            .build();
        client.sendRawEmail(request);
      }
    } else {
      String message = builder.toString();
      Assert.assertEquals(readResource(name), message);
    }
  }

  @Test
  public void simpleTextMessage() throws IOException {
    checkMessage("simpleTextMessage", new EmailRequestBuilder()
        .withFromAddress(FROM)
        .withToAddresses(TO)
        .withSubject("Test message")
        .withTextBody("Body of the email message."));
  }

  @Test
  public void simpleHtmlMessage() throws IOException {
    checkMessage("simpleHtmlMessage", new EmailRequestBuilder()
        .withFromAddress(FROM)
        .withToAddresses(TO)
        .withSubject("Test message")
        .withHtmlBody("<html><body><h1>Test</h1><p>Body of the email message.</p></body></html>"));
  }

  @Test
  public void sendingAuthorizationFromArn() throws IOException {
    checkMessage("sendingAuthorizationFromArn", new EmailRequestBuilder()
        .withFromArn(FROM_ARN)
        .withFromAddress(FROM)
        .withToAddresses(TO)
        .withSubject("Test message")
        .withTextBody("Body of the email message."));
  }

  @Test
  public void sendingAuthorizationRequestParams() throws IOException {
    // Check that From can be omitted. Must be specified by calling withSource on the
    // SendRawEmailRequestObject. Otherwise it will fail with:
    //
    // AmazonSimpleEmailServiceException: Missing required header 'From'.
    checkMessage("sendingAuthorizationRequestParams", new EmailRequestBuilder()
        .withToAddresses(TO)
        .withSubject("Test message")
        .withTextBody("Body of the email message."));
  }

  @Test
  public void withCC() throws IOException {
    checkMessage("withCC", new EmailRequestBuilder()
        .withFromAddress(FROM)
        .withToAddresses(TO)
        .withCcAddresses(CC)
        .withSubject("Test message")
        .withTextBody("Body of the email message."));
  }

  @Test
  public void withBCC() throws IOException {
    checkMessage("withBCC", new EmailRequestBuilder()
        .withFromAddress(FROM)
        .withToAddresses(TO)
        .withBccAddresses(CC)
        .withSubject("Test message")
        .withTextBody("Body of the email message."));
  }

  @Test
  public void withReplyTo() throws IOException {
    checkMessage("withReplyTo", new EmailRequestBuilder()
        .withFromAddress(FROM)
        .withToAddresses(TO)
        .withReplyToAddresses(CC)
        .withSubject("Test message")
        .withTextBody("Body of the email message."));
  }

  @Test
  public void longSubject() throws IOException {
    // https://tools.ietf.org/html/rfc5322#section-2.1.1
    String base = "Test message with a long repeated subject. ";
    String subject = base + base + base + base + base + base + base;
    checkMessage("longSubject", new EmailRequestBuilder()
        .withFromAddress(FROM)
        .withToAddresses(TO)
        .withSubject(subject)
        .withTextBody("Body of the email message."));
  }

  @Test
  public void tooLongSubject() throws IOException {
    // https://tools.ietf.org/html/rfc5322#section-2.1.1
    String base = "Test message with a long repeated subject.";
    String subject = base;
    while (subject.length() < 998) {
      subject += " " + base;
    }
    checkMessage("tooLongSubject", new EmailRequestBuilder()
        .withFromAddress(FROM)
        .withToAddresses(TO)
        .withSubject(subject)
        .withTextBody("Body of the email message."));
  }

  @Test
  public void tooLongSubjectNoSpace() throws IOException {
    // https://tools.ietf.org/html/rfc5322#section-2.1.1
    String base = "Too_long_subject_no_space__";
    String subject = base;
    while (subject.length() < 256) {
      subject += base;
    }
    checkMessage("tooLongSubjectNoSpace", new EmailRequestBuilder()
        .withFromAddress(FROM)
        .withToAddresses(TO)
        .withSubject(subject)
        .withTextBody("Body of the email message."));
  }

  @Test
  public void subjectWithUtf8() throws IOException {
    String subject = "警报";
    checkMessage("subjectWithUtf8", new EmailRequestBuilder()
        .withFromAddress(FROM)
        .withToAddresses(TO)
        .withSubject(subject)
        .withTextBody("Body of the email message."));
  }

  @Test
  public void bodyWithUtf8() throws IOException {
    checkMessage("bodyWithUtf8", new EmailRequestBuilder()
        .withFromAddress(FROM)
        .withToAddresses(TO)
        .withSubject("Test message")
        .withTextBody("Body of the email message contains UTF-8 chars: 警报."));
  }

  @Test
  public void htmlWithInlineImage() throws IOException {
    checkMessage("htmlWithInlineImage", new EmailRequestBuilder()
        .withFromAddress(FROM)
        .withToAddresses(TO)
        .withSubject("Test message")
        .withHtmlBody("<html><body><h1>Alert!</h1><p><img src=\"cid:des-example.png\" /></p></body></html>")
        .addAttachment(Attachment.fromResource("image/png", "des-example.png")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void missingRecipients() {
    EmailRequestBuilder builder = new EmailRequestBuilder()
        .withFromAddress(FROM)
        .withSubject("Test message")
        .withTextBody("Body of the email message.");
    builder.toString();
  }

  @Test(expected = IllegalArgumentException.class)
  public void missingSubject() {
    EmailRequestBuilder builder = new EmailRequestBuilder()
        .withFromAddress(FROM)
        .withToAddresses(TO)
        .withTextBody("Body of the email message.");
    builder.toString();
  }

  @Test
  public void sesConfigSet() throws IOException {
    checkMessage("sesConfigSet", new EmailRequestBuilder()
        .withFromAddress(FROM)
        .withToAddresses(TO)
        .withConfigSet("emailTracking")
        .withSubject("Test message")
        .withHtmlBody("Repo <a href=\"https://github.com/Netflix/iep/\">repo</a>."));
  }

  @Test
  public void customHeader() throws IOException {
    checkMessage("customHeader", new EmailRequestBuilder()
        .withFromAddress(FROM)
        .withToAddresses(TO)
        .addHeader("X-Custom-Header", "Custom header value with UTF-8 characters: 警报")
        .withSubject("Test message")
        .withHtmlBody("Repo <a href=\"https://github.com/Netflix/iep/\">repo</a>."));
  }

  @Test
  public void customHeaderMaxLength() throws IOException {
    StringBuilder builder = new StringBuilder().append("X-");
    for (int i = 0; i < 72; ++i) {
      builder.append('a');
    }
    checkMessage("customHeaderMaxLength", new EmailRequestBuilder()
        .withFromAddress(FROM)
        .withToAddresses(TO)
        .addHeader(builder.toString(), "Custom header value with UTF-8 characters: 警报")
        .withSubject("Test message")
        .withHtmlBody("Repo <a href=\"https://github.com/Netflix/iep/\">repo</a>."));
  }

  @Test(expected = IllegalArgumentException.class)
  public void customHeaderTooLong() throws IOException {
    StringBuilder builder = new StringBuilder().append("X-");
    for (int i = 0; i < 73; ++i) {
      builder.append('a');
    }
    new EmailRequestBuilder()
        .withFromAddress(FROM)
        .withToAddresses(TO)
        .addHeader(builder.toString(), "Custom header value with UTF-8 characters: 警报");
  }

  @Test
  public void customHeaderLongValue() throws IOException {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < 50; ++i) {
      builder.append("Custom header value with UTF-8 characters: 警报. ");
    }
    checkMessage("customHeaderLongValue", new EmailRequestBuilder()
        .withFromAddress(FROM)
        .withToAddresses(TO)
        .addHeader("X-Custom-Header", builder.toString())
        .withSubject("Test message")
        .withHtmlBody("Repo <a href=\"https://github.com/Netflix/iep/\">repo</a>."));
  }

  @Test(expected = IllegalArgumentException.class)
  public void customHeaderInvalidValue() throws IOException {
    new EmailRequestBuilder()
        .withFromAddress(FROM)
        .withToAddresses(TO)
        .addHeader("X Custom Header", "Message");
  }

  @Test(expected = IllegalArgumentException.class)
  public void customHeaderBlacklisted() throws IOException {
    new EmailRequestBuilder()
        .withFromAddress(FROM)
        .withToAddresses(TO)
        .addHeader("Subject", "Message");
  }

  @Test
  public void builderToSdkBytes() throws Exception {
    EmailRequestBuilder builder = new EmailRequestBuilder()
        .withFromAddress(FROM)
        .withToAddresses(TO)
        .withSubject("Test message")
        .withHtmlBody("<html><body><h1>Alert!</h1><p><img src=\"cid:des-example.png\"></p></body></html>")
        .addAttachment(Attachment.fromResource("image/png", "des-example.png"));
    SdkBytes buffer = SdkBytes.fromByteBuffer(builder.toByteBuffer());
    SdkBytes bytes = SdkBytes.fromByteArray(builder.toByteArray());
    SdkBytes string = SdkBytes.fromUtf8String(builder.toString());
    Assert.assertEquals(buffer, bytes);
    Assert.assertEquals(buffer, string);
  }
}
