/*
 * Copyright 2014-2019 Netflix, Inc.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spectator.ipc.http.HttpClient;
import com.netflix.spectator.ipc.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Load server groups from Edda. Queries the {@code /netflix/serverGroups} endpoint to get all
 * server groups across both EC2 and Titus. This data is based on Edda's cache of AWS and Titus
 * data.
 */
public class EddaLoader implements Loader {

  private static final Logger LOGGER = LoggerFactory.getLogger(EddaLoader.class);

  private final HttpClient client;
  private final URI uri;

  private final ObjectMapper mapper;

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
    this.mapper = new ObjectMapper();
  }

  private String getString(JsonNode node, String field) {
    JsonNode value = node.get(field);
    return value == null ? null : value.textValue();
  }

  private int getInt(JsonNode node, String field) {
    JsonNode value = node.get(field);
    return value == null ? 0 : value.asInt();
  }

  private Instance decodeInstance(JsonNode instance) {
    return Instance.builder()
        .node(getString(instance, "node"))
        .privateIpAddress(getString(instance, "privateIpAddress"))
        .vpcId(getString(instance, "vpcId"))
        .subnetId(getString(instance, "subnetId"))
        .ami(getString(instance, "ami"))
        .vmtype(getString(instance, "vmtype"))
        .zone(getString(instance, "zone"))
        .status(Instance.Status.NOT_REGISTERED)
        .build();
  }

  private List<Instance> decodeInstances(JsonNode entries) {
    if (entries == null) {
      return Collections.emptyList();
    }

    List<Instance> instances = new ArrayList<>();
    Iterator<JsonNode> iter = entries.elements();
    while (iter.hasNext()) {
      instances.add(decodeInstance(iter.next()));
    }
    return instances;
  }

  private ServerGroup decodeServerGroup(JsonNode group) {
    return ServerGroup.builder()
        .platform(getString(group, "platform"))
        .group(getString(group, "group"))
        .minSize(getInt(group, "minSize"))
        .maxSize(getInt(group, "maxSize"))
        .desiredSize(getInt(group, "desiredSize"))
        .addInstances(decodeInstances(group.get("instances")))
        .build();
  }

  private List<ServerGroup> decodeServerGroups(JsonNode entries) {
    List<ServerGroup> groups = new ArrayList<>();
    Iterator<JsonNode> iter = entries.elements();
    while (iter.hasNext()) {
      groups.add(decodeServerGroup(iter.next()));
    }
    return groups;
  }

  @Override public List<ServerGroup> call() throws Exception {

    HttpResponse response = client.get(uri)
        .customizeLogging(entry -> entry.withEndpoint("/api/v2/netflix/serverGroups"))
        .acceptGzip()
        .acceptJson()
        .send()
        .decompress();

    if (response.status() != 200) {
      throw new IOException("request failed with status " + response.status());
    }

    return decodeServerGroups(mapper.readTree(response.entity()));
  }
}
