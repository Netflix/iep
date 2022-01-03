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
package com.netflix.iep.servergroups;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper functions for deserializing JSON using Jackson's streaming API.
 */
final class JsonUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(JsonUtils.class);

  private static boolean isEndOfArrayOrInput(JsonParser jp) {
    JsonToken t = jp.getCurrentToken();
    return t == null || t == JsonToken.END_ARRAY;
  }

  private static boolean isEndOfObjectOrInput(JsonParser jp) {
    JsonToken t = jp.getCurrentToken();
    return t == null || t == JsonToken.END_OBJECT;
  }

  /** Check that the current token for the parser is of the expected type. */
  private static void expect(JsonParser jp, JsonToken expected) throws IOException {
    JsonToken actual = jp.getCurrentToken();
    if (actual != expected) {
      JsonLocation loc = jp.getCurrentLocation();
      throw new IllegalArgumentException(
          "invalid input: expected " + expected + ", received " + actual
              + " (line " + loc.getLineNr() + ", column " + loc.getColumnNr() + ")");
    }
  }

  /** Create a list from the current array value. */
  static <T> List<T> toList(JsonParser jp, IOFunction<T> f) throws IOException {
    if (jp.getCurrentToken() == JsonToken.VALUE_NULL) {
      return Collections.emptyList();
    }
    List<T> vs = new ArrayList<>();
    forEach(jp, p -> vs.add(f.apply(p)));
    return vs;
  }

  /** Apply the consumer function for each element in the array. */
  static <T> void forEach(JsonParser jp, IOConsumer f) throws IOException {
    expect(jp, JsonToken.START_ARRAY);
    jp.nextToken();
    while (!isEndOfArrayOrInput(jp)) {
      f.apply(jp);
    }
    if (jp.getCurrentToken() == JsonToken.END_ARRAY) {
      jp.nextToken();
    }
  }

  /** Apply the consumer function for each field in the object. */
  static <T> void forEachField(JsonParser jp, IOBiConsumer f) throws IOException {
    expect(jp, JsonToken.START_OBJECT);
    jp.nextToken();
    while (!isEndOfObjectOrInput(jp)) {
      expect(jp, JsonToken.FIELD_NAME);
      jp.nextToken();
      f.apply(jp.getCurrentName(), jp);
    }
    if (jp.getCurrentToken() == JsonToken.END_OBJECT) {
      jp.nextToken();
    }
  }

  /** Extract a string value. */
  static String stringValue(JsonParser jp) throws IOException {
    if (jp.getCurrentToken() == JsonToken.VALUE_NULL) {
      return null;
    }
    expect(jp, JsonToken.VALUE_STRING);
    String v = jp.getText();
    jp.nextToken();
    return v;
  }

  /** Extract an integer value. */
  static int intValue(JsonParser jp) throws IOException {
    expect(jp, JsonToken.VALUE_NUMBER_INT);
    int v = -1;
    try {
      v = jp.getIntValue();
    } catch (JsonProcessingException e) {
      LOGGER.warn("failed to parse value as integer", e);
    }
    jp.nextToken();
    return v;
  }

  /**
   * Skip the current JSON value. For arrays and objects it will skip over all of the
   * tokens and set the position to the token after the end of the array or object.
   */
  static void skipValue(JsonParser jp) throws IOException {
    if (jp.getCurrentToken() == null) {
      return;
    }
    switch (jp.getCurrentToken()) {
      case START_ARRAY:
      case START_OBJECT:
        jp.skipChildren();
        jp.nextToken();
        break;
      default:
        jp.nextToken();
        break;
    }
  }

  /** Function that maps the current input of the parser to an object of type {@code T}. */
  interface IOFunction<T> {
    T apply(JsonParser input) throws IOException;
  }

  /** Consumer that handles current input of the parser. */
  interface IOConsumer {
    void apply(JsonParser input) throws IOException;
  }

  /** Consumer that handles current input of the parser for a given field. */
  interface IOBiConsumer {
    void apply(String field, JsonParser jp) throws IOException;
  }
}
