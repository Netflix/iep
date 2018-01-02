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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Attachment data to add into the email. Keep in mind that there are
 * <a href="http://docs.aws.amazon.com/ses/latest/DeveloperGuide/mime-types.html">restrictions</a>
 * on the type of files that are allowed as attachments. There are also
 * <a href="http://docs.aws.amazon.com/ses/latest/DeveloperGuide/limits.html">limits</a> on
 * the overall size of a message.
 */
public class Attachment {

  /** Create an attachment based on the contents of a file. */
  public static Attachment fromFile(String contentType, File file) throws IOException {
    try (InputStream in = new FileInputStream(file)) {
      return fromInputStream(file.getName(), contentType, in);
    }
  }

  /**
   * Create an attachment based on the contents of a resource in the classpath. This method
   * will use the context class loader of the current thread.
   */
  public static Attachment fromResource(String contentType, String resource) throws IOException {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) {
      // Fallback to class loader for this class if we cannot get a context class loader
      cl = Attachment.class.getClassLoader();
    }
    return fromResource(cl, contentType, resource);
  }

  /** Create an attachment based on the contents of a resource in the classpath. */
  public static Attachment fromResource(ClassLoader cl, String contentType, String resource)
      throws IOException {
    try (InputStream in = cl.getResourceAsStream(resource)) {
      File f = new File(resource);
      return fromInputStream(f.getName(), contentType, in);
    }
  }

  /** Create an attachment based on the contents read from an input stream. */
  public static Attachment fromInputStream(String name, String contentType, InputStream in)
      throws IOException {
    return fromByteArray(name, contentType, EncodingUtils.readAll(in));
  }

  /** Create an attachment based on the contents of the byte array. */
  public static Attachment fromByteArray(String name, String contentType, byte[] data) {
    return new Attachment(name, contentType, data);
  }

  private final String disposition;
  private final String name;
  private final String contentType;
  private final byte[] data;

  /** Create a new attachment instance. */
  private Attachment(String name, String contentType, byte[] data) {
    this.disposition = "attachment";
    this.name = name;
    this.contentType = contentType;
    this.data = data;
  }

  @Override public String toString() {
    return new StringBuilder()
        .append(EmailHeader.contentType(contentType).toString())
        .append(EmailHeader.contentTransferEncoding("base64").toString())
        .append(EmailHeader.contentDisposition(disposition).toString())
        .append(EmailHeader.contentID(name).toString())
        .append(EncodingUtils.CRLF)
        .append(EncodingUtils.base64(data))
        .append(EncodingUtils.CRLF)
        .toString();
  }
}
