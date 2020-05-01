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

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Header to include in the email. */
class EmailHeader {

  private static final Set<String> DENYLIST;
  static {
    Set<String> denylist = new HashSet<>();
    denylist.add("mime-version");
    denylist.add("content-type");
    denylist.add("content-disposition");
    denylist.add("content-transfer-encoding");
    denylist.add("content-id");
    denylist.add("reply-to");
    denylist.add("from");
    denylist.add("to");
    denylist.add("cc");
    denylist.add("bcc");
    denylist.add("subject");
    denylist.add("x-ses-from-arn");
    denylist.add("x-ses-configuration-set");
    DENYLIST = Collections.unmodifiableSet(denylist);
  }

  /** Checks if a header is allowed to be set by the user or is blacklisted. */
  static void checkCustomHeader(String header) {
    if (DENYLIST.contains(header.toLowerCase(Locale.US))) {
      throw new IllegalArgumentException("'" + header + "' cannot be used as a custom header, " +
          "use the appropriate method on the builder");
    }

    // Check that only allowed characters are used in the header
    for (int i = 0; i < header.length(); ++i) {
      if (!isAllowedInFieldName(header.charAt(i))) {
        throw new IllegalArgumentException("'" + header + "' contains an invalid character: '" +
            header.charAt(i) + "'");
      }
    }

    // Check max length of header, lines should be 78 characters or less including CRLF. Since
    // the header cannot be folded this means max length of 74 (78 - len(': ') - len(CRLF)).
    // https://tools.ietf.org/html/rfc5322#section-2.2.3
    if (header.length() > 74) {
      throw new IllegalArgumentException("'" + header + "' header exceeds max length of 74");
    }
  }

  /**
   * Returns true if the character is allowed for use within the field name of an email
   * header according to <a href="https://tools.ietf.org/html/rfc5322#section-2.2">RFC-5322</a>.
   */
  static boolean isAllowedInFieldName(char c) {
    return c >= 33 && c <= 126 && c != ':';
  }

  /**
   * Create a new custom header instance. The values will be base64 encoded and wrapped
   * to fit email restrictions.
   */
  static EmailHeader custom(String key, String value) {
    // Add 2 to key length to account for ': ' between key and value
    return new EmailHeader(key, EncodingUtils.wrapBase64(value, key.length() + 2, 50));
  }

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

  /** Content type header for a part of the message. */
  static EmailHeader contentType(String type, String name) {
    return new EmailHeader("Content-Type", type + "; name=\"" + name + "\"");
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

  /** Identity associated with the sending authorization policy. */
  static EmailHeader fromArn(String arn) {
    return new EmailHeader("X-SES-FROM-ARN", arn);
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

  /** SES configuration set header. */
  static EmailHeader configSet(String name) {
    return new EmailHeader("X-SES-CONFIGURATION-SET", name);
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
