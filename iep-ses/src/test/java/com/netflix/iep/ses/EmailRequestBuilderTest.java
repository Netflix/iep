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

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@RunWith(JUnit4.class)
public class EmailRequestBuilderTest {

  private static boolean BLESS = false;
  private static boolean SEND = false;

  private static String FROM = "bob@example.com";
  private static String TO = "andrew@example.com";
  private static String CC = "sue@example.com";

  private static String BOUNDARY = "331239ab-8a31-4cc6-84d6-5557f96ebc3a";

  private void writeResource(String name, byte[] data) throws IOException {
    // Path ends with: iep-ses/target/test-classes/des-example.png
    URL url = Thread.currentThread().getContextClassLoader().getResource("des-example.png");
    File projectDir = (new File(url.getPath()))
        .getParentFile()
        .getParentFile()
        .getParentFile();
    File resourceDir = new File(projectDir, "src/test/resources");

    // Sanity check that resource dir exists
    if (!resourceDir.isDirectory()) {
      throw new IOException("cannot find resource directory: " + resourceDir);
    }

    // Write data to file
    try (OutputStream out = new FileOutputStream(new File(resourceDir, name))) {
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
      AmazonSimpleEmailService client = AmazonSimpleEmailServiceClient.builder()
          .withCredentials(new DefaultAWSCredentialsProviderChain())
          .withRegion("us-east-1")
          .build();
      client.sendRawEmail(builder.build());
    } else {
      String message = builder.toString();
      Assert.assertEquals(readResource(name), message);
    }
  }

  @Test
  public void simpleTextMessage() throws IOException {
    checkMessage("simpleTextMessage", new EmailRequestBuilder()
        .withSource(FROM)
        .withToAddresses(TO)
        .withSubject("Test message")
        .withTextBody("Body of the email message."));
  }

  @Test
  public void simpleHtmlMessage() throws IOException {
    checkMessage("simpleHtmlMessage", new EmailRequestBuilder()
        .withSource(FROM)
        .withToAddresses(TO)
        .withSubject("Test message")
        .withHtmlBody("<html><body><h1>Test</h1><p>Body of the email message.</p></body></html>"));
  }

  @Test
  public void withCC() throws IOException {
    checkMessage("withCC", new EmailRequestBuilder()
        .withSource(FROM)
        .withToAddresses(TO)
        .withCcAddresses(CC)
        .withSubject("Test message")
        .withTextBody("Body of the email message."));
  }

  @Test
  public void withBCC() throws IOException {
    checkMessage("withBCC", new EmailRequestBuilder()
        .withSource(FROM)
        .withToAddresses(TO)
        .withBccAddresses(CC)
        .withSubject("Test message")
        .withTextBody("Body of the email message."));
  }

  @Test
  public void usingDestination() throws IOException {
    checkMessage("simpleTextMessage", new EmailRequestBuilder()
        .withSource(FROM)
        .withDestination(new Destination().withToAddresses(TO))
        .withSubject("Test message")
        .withTextBody("Body of the email message."));
  }

  @Test
  public void usingMessageText() throws IOException {
    Message message = new Message()
        .withSubject(new Content("Test message"))
        .withBody(new Body().withText(new Content("Body of the email message.")));
    checkMessage("simpleTextMessage", new EmailRequestBuilder()
        .withSource(FROM)
        .withToAddresses(TO)
        .withMessage(message));
  }

  @Test
  public void usingMessageHtml() throws IOException {
    Message message = new Message()
        .withSubject(new Content("Test message"))
        .withBody(new Body()
            .withText(new Content("Body of the email message."))
            .withHtml(new Content("<html><body><h1>Test</h1><p>Body of the email message.</p></body></html>"))
        );
    checkMessage("simpleHtmlMessage", new EmailRequestBuilder()
        .withSource(FROM)
        .withToAddresses(TO)
        .withMessage(message));
  }

  @Test
  public void withReplyTo() throws IOException {
    checkMessage("withReplyTo", new EmailRequestBuilder()
        .withSource(FROM)
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
        .withSource(FROM)
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
        .withSource(FROM)
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
        .withSource(FROM)
        .withToAddresses(TO)
        .withSubject(subject)
        .withTextBody("Body of the email message."));
  }

  @Test
  public void subjectWithUtf8() throws IOException {
    String subject = "警报";
    checkMessage("subjectWithUtf8", new EmailRequestBuilder()
        .withSource(FROM)
        .withToAddresses(TO)
        .withSubject(subject)
        .withTextBody("Body of the email message."));
  }

  @Test
  public void bodyWithUtf8() throws IOException {
    checkMessage("bodyWithUtf8", new EmailRequestBuilder()
        .withSource(FROM)
        .withToAddresses(TO)
        .withSubject("Test message")
        .withTextBody("Body of the email message contains UTF-8 chars: 警报."));
  }

  @Test
  public void htmlWithInlineImage() throws IOException {
    checkMessage("htmlWithInlineImage", new EmailRequestBuilder()
        .withSource(FROM)
        .withToAddresses(TO)
        .withSubject("Test message")
        .withHtmlBody("<html><body><h1>Alert!</h1><p><img src=\"cid:des-example.png\" /></p></body></html>")
        .addAttachment(Attachment.fromResource("image/png", "des-example.png")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void missingRecipients() {
    EmailRequestBuilder builder = new EmailRequestBuilder()
        .withSource(FROM)
        .withSubject("Test message")
        .withTextBody("Body of the email message.");
    builder.toString();
  }

  @Test(expected = IllegalArgumentException.class)
  public void missingFrom() {
    EmailRequestBuilder builder = new EmailRequestBuilder()
        .withToAddresses(TO)
        .withSubject("Test message")
        .withTextBody("Body of the email message.");
    builder.toString();
  }

  @Test(expected = IllegalArgumentException.class)
  public void missingSubject() {
    EmailRequestBuilder builder = new EmailRequestBuilder()
        .withSource(FROM)
        .withToAddresses(TO)
        .withTextBody("Body of the email message.");
    builder.toString();
  }
}
