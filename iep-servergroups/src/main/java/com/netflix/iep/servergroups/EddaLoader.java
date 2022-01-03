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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.netflix.spectator.ipc.http.HttpClient;
import com.netflix.spectator.ipc.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Load server groups from Edda. Queries the {@code /netflix/serverGroups} endpoint to get all
 * server groups across both EC2 and Titus. This data is based on Edda's cache of AWS and Titus
 * data.
 */
public class EddaLoader implements Loader {

  private static final Logger LOGGER = LoggerFactory.getLogger(EddaLoader.class);

  private final HttpClient client;
  private final URI uri;

  private final JsonFactory jsonFactory;

  /**
   * Create a new instance.
   *
   * @param client
   *     HTTP client used to request data from Edda.
   * @param uri
   *     Full URI to the {@code /netflix/serverGroups} endpoint on the Edda server.
   */
  public EddaLoader(HttpClient client, URI uri) {
    this.client = client;
    this.uri = uri;
    this.jsonFactory = new JsonFactory();
  }

  private Instance decodeInstance(JsonParser jp) throws IOException {
    Instance.Builder builder = Instance.builder().status(Instance.Status.NOT_REGISTERED);
    JsonUtils.forEachField(jp, (field, p) -> {
      switch (field) {
        case "node":
          builder.node(JsonUtils.stringValue(jp));
          break;
        case "privateIpAddress":
          builder.privateIpAddress(JsonUtils.stringValue(jp));
          break;
        case "vpcId":
          builder.vpcId(JsonUtils.stringValue(jp));
          break;
        case "subnetId":
          builder.subnetId(JsonUtils.stringValue(jp));
          break;
        case "ami":
          builder.ami(JsonUtils.stringValue(jp));
          break;
        case "vmtype":
          builder.vmtype(JsonUtils.stringValue(jp));
          break;
        case "zone":
          builder.zone(JsonUtils.stringValue(jp));
          break;
        default:
          // Ignore unknown fields
          JsonUtils.skipValue(jp);
          break;
      }
    });
    return builder.build();
  }

  private List<Instance> decodeInstances(JsonParser jp) throws IOException {
    if (jp.getCurrentToken() == JsonToken.VALUE_NULL) {
      return Collections.emptyList();
    }
    List<Instance> vs = new ArrayList<>();
    JsonUtils.forEach(jp, p -> {
      try {
        vs.add(decodeInstance(jp));
      } catch (NullPointerException e) {
        // Log but otherwise ignore failures like missing IP address
        LOGGER.warn("failed to process instance in Edda response", e);
      }
    });
    return vs;
  }

  private ServerGroup decodeServerGroup(JsonParser jp) throws IOException {
    ServerGroup.Builder builder = ServerGroup.builder();
    JsonUtils.forEachField(jp, (field, p) -> {
      switch (field) {
        case "platform":
          builder.platform(JsonUtils.stringValue(jp));
          break;
        case "group":
          builder.group(JsonUtils.stringValue(jp));
          break;
        case "minSize":
          builder.minSize(JsonUtils.intValue(jp));
          break;
        case "maxSize":
          builder.maxSize(JsonUtils.intValue(jp));
          break;
        case "desiredSize":
          builder.desiredSize(JsonUtils.intValue(jp));
          break;
        case "instances":
          builder.addInstances(decodeInstances(jp));
          break;
        default:
          // Ignore unknown fields
          JsonUtils.skipValue(jp);
          break;
      }
    });
    return builder.build();
  }

  private List<ServerGroup> decodeServerGroups(JsonParser jp) throws IOException {
    jp.nextToken();
    return JsonUtils.toList(jp, this::decodeServerGroup);
  }

  @Override public List<ServerGroup> call() throws Exception {

    HttpResponse response = client.get(uri)
        .customizeLogging(entry -> entry.withEndpoint("/api/v2/netflix/serverGroups"))
        .acceptGzip()
        .acceptJson()
        .send();

    if (response.status() != 200) {
      throw new IOException("request failed with status " + response.status());
    }

    String enc = response.header("Content-Encoding");
    JsonParser jp = (enc != null && enc.contains("gzip"))
        ? jsonFactory.createParser(new GZIPInputStream(new ByteArrayInputStream(response.entity())))
        : jsonFactory.createParser(response.entity());
    return decodeServerGroups(jp);
  }
}
