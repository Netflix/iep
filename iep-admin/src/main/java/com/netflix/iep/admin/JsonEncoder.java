/*
 * Copyright 2014-2025 Netflix, Inc.
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
package com.netflix.iep.admin;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Keeps a cached copy of the mapper to reuse.
 */
class JsonEncoder {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final JsonFactory FACTORY = new JsonFactory();

  @SuppressWarnings("unchecked")
  static void encode(Object obj, OutputStream out) throws IOException {
    if (obj instanceof Iterable<?>) {
      Iterable<Object> values = (Iterable<Object>) obj;
      try (JsonGenerator gen = FACTORY.createGenerator(out)) {
        gen.writeStartArray();
        for (Object value : values) {
          MAPPER.writeValue(gen, value);
        }
        gen.writeEndArray();
      }
    } else {
      MAPPER.writeValue(out, obj);
    }
  }
}
