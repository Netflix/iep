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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Load server groups from Eureka. Queries the {@code /v2/apps} endpoint to get all apps
 * and then maps this into a set of server groups. Since this is based on the data published
 * to Eureka, it may be incorrect if the user configures their app with bad metadata. It
 * can be used as a backup if data coming from other sources is stale.
 */
public class EurekaLoader implements Loader {

  private static final Logger LOGGER = LoggerFactory.getLogger(EurekaLoader.class);

  private final HttpClient client;
  private final URI uri;
  private final Predicate<String> accounts;

  private final ObjectMapper mapper;

  /**
   * Create a new instance.
   *
   * @param client
   *     HTTP client used to request data from Eureka.
   * @param uri
   *     Full URI to the {@code /apps} endpoint on the Eureka server.
   * @param accounts
   *     Condition used to filter the Eureka results by account. Eureka potentially has
   *     registrations from multiple accounts, however, to join with sources such as Edda
   *     or direct from AWS we need only data from the corresponding accounts. This condition
   *     should be set to match the set of accounts covered by other loaders being used.
   */
  public EurekaLoader(HttpClient client, URI uri, Predicate<String> accounts) {
    this.client = client;
    this.uri = uri;
    this.accounts = accounts;
    this.mapper = new ObjectMapper();
  }

  private Instance.Status getStatus(JsonNode instance) {
    String status = instance.get("status").textValue();
    return Instance.Status.valueOf(status);
  }

  private String getInstanceId(JsonNode instance) {
    JsonNode id = instance.get("instanceId");
    return id == null
        ? getMetadataField(instance, "instance-id")
        : id.textValue();
  }

  private String getPrivateIp(JsonNode instance) {
    JsonNode ip = instance.get("ipAddr");
    return ip == null
        ? getMetadataField(instance, "local-ipv4")
        : ip.textValue();
  }

  private String getMetadataField(JsonNode instance, String key) {
    JsonNode node = instance.path("dataCenterInfo").path("metadata").path(key);
    return node.isMissingNode() ? null : node.textValue();
  }

  private String getInstanceType(JsonNode instance) {
    String vmtype = getMetadataField(instance, "instance-type");
    return "Titus".equals(vmtype) ? null : vmtype;
  }

  private Instance decodeInstance(JsonNode instance) {
    String account = getMetadataField(instance, "accountId");
    if (!accounts.test(account)) {
      return null;
    }

    String node = getInstanceId(instance);
    String privateIp = getPrivateIp(instance);
    if (node == null || privateIp == null) {
      return null;
    }

    return Instance.builder()
        .node(node)
        .privateIpAddress(privateIp)
        .vpcId(getMetadataField(instance, "vpc-id"))
        .ami(getMetadataField(instance, "ami-id"))
        .vmtype(getInstanceType(instance))
        .zone(getMetadataField(instance, "availability-zone"))
        .status(getStatus(instance))
        .build();
  }

  private Map<GroupId, Set<Instance>> decodeApp(JsonNode app) {
    Map<GroupId, Set<Instance>> instances = new HashMap<>();
    JsonNode instancesArray = app.get("instance");
    if (instancesArray == null) {
      return instances;
    }

    Iterator<JsonNode> iter = instancesArray.elements();
    while (iter.hasNext()) {
      JsonNode instance = iter.next();
      JsonNode group = instance.get("asgName");
      if (group != null) {
        Instance instanceObj = decodeInstance(instance);
        if (instanceObj != null) {
          String platform = instanceObj.getNode().startsWith("i-") ? "ec2" : "titus";
          GroupId id = new GroupId(platform, group.textValue());
          instances.computeIfAbsent(id, k -> new HashSet<>()).add(instanceObj);
        } else {
          LOGGER.trace("failed to decode instance: {}", instance);
        }
      }
    }
    return instances;
  }

  private void merge(Map<GroupId, Set<Instance>> i1, Map<GroupId, Set<Instance>> i2) {
    for (Map.Entry<GroupId, Set<Instance>> entry : i2.entrySet()) {
      i1.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).addAll(entry.getValue());
    }
  }

  private List<ServerGroup> decodeAppList(JsonNode apps) {
    Map<GroupId, Set<Instance>> instances = new HashMap<>();
    Iterator<JsonNode> iter = apps.elements();
    while (iter.hasNext()) {
      merge(instances, decodeApp(iter.next()));
    }

    List<ServerGroup> groups = new ArrayList<>();
    for (Map.Entry<GroupId, Set<Instance>> entry : instances.entrySet()) {
      GroupId id = entry.getKey();
      groups.add(ServerGroup.builder()
          .platform(id.platform)
          .group(id.group)
          .addInstances(entry.getValue())
          .build());
    }

    return groups;
  }

  @Override public List<ServerGroup> call() throws Exception {

    HttpResponse response = client.get(uri)
        .customizeLogging(entry -> entry.withEndpoint("/eureka/v2/apps"))
        .acceptGzip()
        .acceptJson()
        .send()
        .decompress();

    if (response.status() != 200) {
      throw new IOException("request failed with status " + response.status());
    }

    JsonNode node = mapper.readTree(response.entity());
    return decodeAppList(node.get("applications").get("application"));
  }

  private static class GroupId {
    private final String platform;
    private final String group;

    private GroupId(String platform, String group) {
      this.platform = platform;
      this.group = group;
    }

    @Override public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      GroupId groupId = (GroupId) o;
      return Objects.equals(platform, groupId.platform) &&
          Objects.equals(group, groupId.group);
    }

    @Override public int hashCode() {
      return Objects.hash(platform, group);
    }
  }
}
