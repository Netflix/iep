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
package com.netflix.iep.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * Server sent event message.
 */
public final class ServerSentEvent {

  private static ByteBuf toByteBuf(String s) {
    try {
      return Unpooled.wrappedBuffer(s.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Parse a server sent event line from the ByteBuf.
   */
  public static ServerSentEvent parse(ByteBuf buf) {
    return parse(buf.toString(Charset.forName("UTF-8")));
  }

  /**
   * Parse a server sent event line from the String.
   */
  public static ServerSentEvent parse(String line) {
    //http://www.w3.org/TR/eventsource/#event-stream-interpretation

    // The line is empty (a blank line)
    //   Dispatch the event, as defined below.
    // If the line starts with a U+003A COLON character (:)
    //   Ignore the line.
    if (line == null || "".equals(line) || line.startsWith(":")) {
      return null;
    }

    int pos = line.indexOf(':');
    if (pos != -1) {
      // The line contains a U+003A COLON character (:)
      //   Collect the characters on the line before the first U+003A COLON character (:), and
      //   let field be that string.
      String field = line.substring(0, pos);

      //   Collect the characters on the line after the first U+003A COLON character (:), and
      //   let value be that string. If value starts with a U+0020 SPACE character, remove it
      //   from value.
      ++pos;
      String value = "";
      if (pos < line.length()) {
        if (line.charAt(pos) == ' ') {
          ++pos;
        }
        value = (pos < line.length()) ? line.substring(pos) : "";
      }
      return new ServerSentEvent(field, value);
    } else {
      // Otherwise, the string is not empty but does not contain a U+003A COLON character (:)
      //   Process the field using the steps described below, using the whole line as the field
      //   name, and the empty string as the field value.
      return new ServerSentEvent(line, "");
    }
  }

  private final String field;
  private final String value;

  /**
   * Create a new instance of a server sent event. Neither field or value can be null.
   */
  public ServerSentEvent(String field, String value) {
    if (field == null) {
      throw new NullPointerException("field");
    }

    if (value == null) {
      throw new NullPointerException("value");
    }

    this.field = field;
    this.value = value;
  }

  /** Return the field for the event. */
  public String field() {
    return field;
  }

  /** Return the value for the event. */
  public String value() {
    return value;
  }

  @Override public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || !(obj instanceof ServerSentEvent)) return false;
    ServerSentEvent other = (ServerSentEvent) obj;
    return field.equals(other.field) && value.equals(other.value);
  }

  @Override public int hashCode() {
    final int prime = 31;
    int hc = prime;
    hc = prime * hc + field.hashCode();
    hc = prime * hc + value.hashCode();
    return hc;

  }

  @Override public String toString() {
    return String.format("%s: %s\n\n", field, value);
  }
}
