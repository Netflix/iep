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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Helper functions for encoding header values and messages.
 */
class EncodingUtils {

  private EncodingUtils() {
  }

  private static final int LINE_LENGTH = 76;

  private static final byte[] LINE_SEP = new byte[] {'\r', '\n'};

  static final String CRLF = "\r\n";

  /** Helper to read all data from InputStream to a byte array. */
  static byte[] readAll(InputStream in) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    int length;
    while ((length = in.read(buffer)) > 0) {
      baos.write(buffer, 0, length);
    }
    return baos.toByteArray();
  }

  /**
   * Wraps the input string to {@code maxLength} characters. If a single word
   * is longer than maxLength it will be on a line by itself and will not be
   * broken up.
   *
   * @param s
   *     Input string to wrap.
   * @param offset
   *     Offset to use for the first line. This method is mostly intended for
   *     headers where the header name and separator need to be considered for
   *     the overall line length.
   * @param maxLength
   *     Maximum desired length for a line.
   * @return
   *     Wrapped string with each line being less than or equal to the specified
   *     maximum length or the maximum word length in the input string.
   */
  static String wrap(String s, int offset, int maxLength) {
    String[] parts = s.split("\\s+");
    StringBuilder builder = new StringBuilder();
    builder.append(parts[0]);
    int length = offset + parts[0].length();
    for (int i = 1; i < parts.length; ++i) {
      String part = parts[i];
      if (length + part.length() + 1 < maxLength) {
        builder.append(' ').append(part);
        length += part.length() + 1;
      } else {
        builder.append("\r\n").append(' ').append(part);
        length = part.length() + 1;
      }
    }
    return builder.toString();
  }

  /**
   * Wraps the input string to encoded lines of at most {@code maxLength}. Each
   * output line will be an <a href="https://tools.ietf.org/html/rfc2047">encoded
   * word</a> using a UTF-8 character set and base64 encoding.
   *
   * @param s
   *     Input string to wrap.
   * @param offset
   *     Offset to use for the first line. This method is mostly intended for
   *     headers where the header name and separator need to be considered for
   *     the overall line length.
   * @param maxLength
   *     Maximum desired length for a line.
   * @return
   *     Wrapped and encoded output string.
   */
  static String wrapBase64(String s, int offset, int maxLength) {
    String prefix = "=?UTF-8?B?";
    String suffix = "?=";

    int length = maxLength - prefix.length() - suffix.length() - 1;
    int start = 0;
    int end = length - offset;
    StringBuilder builder = new StringBuilder();
    do {
      String part = s.substring(start, Math.min(end, s.length()));
      builder.append(EncodingUtils.CRLF)
          .append(' ')
          .append(prefix)
          .append(base64(part.getBytes(StandardCharsets.UTF_8)))
          .append(suffix);
      start = end;
      end += length;
    } while (start < s.length());

    return builder.toString().trim();
  }

  /** Base64 encode the input data. */
  static String base64(byte[] data) {
    Base64.Encoder b64enc = Base64.getMimeEncoder(LINE_LENGTH, LINE_SEP);
    return b64enc.encodeToString(data);
  }
}
