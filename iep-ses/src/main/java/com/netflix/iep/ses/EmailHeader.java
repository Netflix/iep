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

/** Header to include in the email. */
class EmailHeader {

  /** Mime version header included by default. */
  static EmailHeader mime() {
    return new EmailHeader("MIME-Version", "1.0");
  }

  /**
   * Root content type header for a multipart message. Boundary is the separator used
   * between the different parts of the message. The passed in string should not include
   * the leading {@code --}.
   */
  static EmailHeader multipart(String boundary) {
    return new EmailHeader("Content-Type", "multipart/mixed;\r\n  boundary=\"" + boundary + "\"");
  }

  /** Content type header for a part of the message. */
  static EmailHeader contentType(String type) {
    return new EmailHeader("Content-Type", type);
  }

  /**
   * Disposition of the message. This is typically used to indicate if something is an
   * attachment or indended to be viewed inline.
   */
  static EmailHeader contentDisposition(String disposition) {
    return new EmailHeader("Content-Disposition", disposition);
  }

  /** Transfer encoding for the content, typically {@code base64}. */
  static EmailHeader contentTransferEncoding(String encoding) {
    return new EmailHeader("Content-Transfer-Encoding", encoding);
  }

  /**
   * Id for a particular attachment. If linking to an attachment in an HTML email message,
   * then the href value would be {@code cid:content-id}.
   */
  static EmailHeader contentID(String id) {
    return new EmailHeader("Content-ID", "<" + id + ">");
  }

  /** Addresses to use if the user replies. */
  static EmailHeader replyTo(String addresses) {
    return new EmailHeader("Reply-To", EncodingUtils.wrap(addresses, "Reply-To: ".length(), 78));
  }

  /** Address of the user sending the message. */
  static EmailHeader from(String address) {
    return new EmailHeader("From", address);
  }

  /** Addresses of the users who should receive the message. */
  static EmailHeader to(String addresses) {
    return new EmailHeader("To", EncodingUtils.wrap(addresses, "To: ".length(), 78));
  }

  /** Addresses of additional users who should receive the message. */
  static EmailHeader cc(String addresses) {
    return new EmailHeader("CC", EncodingUtils.wrap(addresses, "CC: ".length(), 78));
  }

  /**
   * Addresses of additional users who should receive the message, but not advertised to
   * other recipients.
   */
  static EmailHeader bcc(String addresses) {
    return new EmailHeader("BCC", EncodingUtils.wrap(addresses, "BCC: ".length(), 78));
  }

  /** Subject of the message. */
  static EmailHeader subject(String subject) {
    return new EmailHeader("Subject", EncodingUtils.wrapBase64(subject, "Subject: ".length(), 50));
  }

  private final String name;
  private final String value;

  private EmailHeader(String name, String value) {
    this.name = name;
    this.value = value;
  }

  @Override public String toString() {
    return name + ": " + value + EncodingUtils.CRLF;
  }
}
