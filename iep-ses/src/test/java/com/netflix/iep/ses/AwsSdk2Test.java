package com.netflix.iep.ses;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import software.amazon.awssdk.core.SdkBytes;

@RunWith(JUnit4.class)
public class AwsSdk2Test {

  @Test
  public void builderToSdkBytes() throws Exception {
    EmailRequestBuilder builder = new EmailRequestBuilder()
        .withFromAddress("alert-do-not-reply@saasmail.netflix.com")
        .withToAddresses("brharrington@netflix.com")
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
