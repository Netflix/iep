/*
 * Copyright 2014-2022 Netflix, Inc.
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
