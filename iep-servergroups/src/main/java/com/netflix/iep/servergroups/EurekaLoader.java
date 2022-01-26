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

import com.fasterxml.jackson.core.JsonParser;
import com.netflix.spectator.ipc.http.HttpClient;
import com.netflix.spectator.ipc.http.HttpResponse;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Load server groups from Eureka. Queries the {@code /v2/apps} endpoint to get all apps
 * and then maps this into a set of server groups. Since this is based on the data published
 * to Eureka, it may be incorrect if the user configures their app with bad metadata. It
 * can be used as a backup if data coming from other sources is stale.
 */
public class EurekaLoader implements Loader {

  // Some environments like Titus can result in arbitrary values for the
  // instance type in the metadata. This pattern is used to check for reasonable
  // values.
  private static final Pattern VM_TYPE_PATTERN = Pattern.compile("^[a-zA-Z0-9]+\\.[a-zA-Z0-9]+$");

  private final HttpClient client;
  private final URI uri;
  private final Predicate<String> accounts;

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
  }

  private void decodeMetadata(InstanceInfo info, JsonParser jp) throws IOException {
    JsonUtils.forEachField(jp, (field, p) -> {
      switch (field) {
        case "accountId":
          info.account = JsonUtils.stringValue(p);
          break;
        case "vpc-id":
          info.builder.vpcId(JsonUtils.stringValue(p));
          break;
        case "ami-id":
          info.builder.ami(JsonUtils.stringValue(p));
          break;
        case "availability-zone":
          info.builder.zone(JsonUtils.stringValue(p));
          break;
        case "local-ipv4":
          if (info.privateIp == null) {
            info.privateIp = JsonUtils.stringValue(p);
          } else {
            JsonUtils.skipValue(p);
          }
          break;
        case "instance-id":
          info.node = JsonUtils.stringValue(p);
          break;
        case "instance-type":
          String vmtype = JsonUtils.stringValue(p);
          if (vmtype != null && VM_TYPE_PATTERN.matcher(vmtype).matches()) {
            info.builder.vmtype(vmtype);
          }
          break;
        default:
          JsonUtils.skipValue(p);
          break;
      }
    });
  }

  private void decodeDataCenterInfo(InstanceInfo info, JsonParser jp) throws IOException {
    JsonUtils.forEachField(jp, (field, p) -> {
      if ("metadata".equals(field)) {
        decodeMetadata(info, p);
      } else {
        JsonUtils.skipValue(p);
      }
    });
  }

  private void decodeInstance(Map<GroupId, Set<Instance>> instances, JsonParser jp) throws IOException {
    InstanceInfo info = new InstanceInfo();
    JsonUtils.forEachField(jp, (field, p) -> {
      switch (field) {
        case "asgName":
          info.group = JsonUtils.stringValue(p);
          break;
        case "ipAddr":
          info.privateIp = JsonUtils.stringValue(p);
          break;
        case "instanceId":
          info.node = JsonUtils.stringValue(p);
          break;
        case "status":
          info.builder.status(Instance.Status.valueOf(JsonUtils.stringValue(p)));
          break;
        case "dataCenterInfo":
          decodeDataCenterInfo(info, p);
          break;
        default:
          JsonUtils.skipValue(p);
          break;
      }
    });


    if (info.group != null && accounts.test(info.account)) {
      Instance instance = info.toInstance();
      if (instance != null) {
        String platform = info.node.startsWith("i-") ? "ec2" : "titus";
        GroupId id = new GroupId(platform, info.group);
        instances.computeIfAbsent(id, k -> new HashSet<>()).add(instance);
      }
    }
  }

  private void decodeInstances(Map<GroupId, Set<Instance>> instances, JsonParser jp) throws IOException {
    JsonUtils.forEach(jp, p -> decodeInstance(instances, p));
  }

  private Map<GroupId, Set<Instance>> decodeApp(JsonParser jp) throws IOException {
    Map<GroupId, Set<Instance>> instances = new HashMap<>();
    JsonUtils.forEachField(jp, (field, p) -> {
      if ("instance".equals(field)) {
        decodeInstances(instances, p);
      } else {
        JsonUtils.skipValue(p);
      }
    });
    return instances;
  }

  private void merge(Map<GroupId, Set<Instance>> i1, Map<GroupId, Set<Instance>> i2) {
    for (Map.Entry<GroupId, Set<Instance>> entry : i2.entrySet()) {
      i1.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).addAll(entry.getValue());
    }
  }

  private void decodeAppList(List<ServerGroup> groups, JsonParser jp) throws IOException {
    Map<GroupId, Set<Instance>> instances = new HashMap<>();
    JsonUtils.forEach(jp, p -> merge(instances, decodeApp(p)));

    for (Map.Entry<GroupId, Set<Instance>> entry : instances.entrySet()) {
      GroupId id = entry.getKey();
      groups.add(ServerGroup.builder()
          .platform(id.platform)
          .group(id.group)
          .addInstances(entry.getValue())
          .build());
    }
  }

  private void decodeApps(List<ServerGroup> groups, JsonParser jp) throws IOException {
    JsonUtils.forEachField(jp, (field, p) -> {
      if ("application".equals(field)) {
        decodeAppList(groups, p);
      } else {
        JsonUtils.skipValue(p);
      }
    });
  }

  private List<ServerGroup> decodeApps(JsonParser jp) throws IOException {
    jp.nextToken();
    List<ServerGroup> groups = new ArrayList<>();
    JsonUtils.forEachField(jp, (field, p) -> {
      if ("applications".equals(field)) {
        decodeApps(groups, p);
      } else {
        JsonUtils.skipValue(p);
      }
    });
    return groups;
  }

  @Override public List<ServerGroup> call() throws Exception {

    HttpResponse response = client.get(uri)
        .customizeLogging(entry -> entry.withEndpoint("/eureka/v2/apps"))
        .acceptGzip()
        .acceptJson()
        .send();

    if (response.status() != 200) {
      throw new IOException("request failed with status " + response.status());
    }

    return JsonUtils.parseResponse(response, this::decodeApps);
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

  private static class InstanceInfo {
    private String group;
    private String account;
    private String node;
    private String privateIp;
    private Instance.Builder builder;

    InstanceInfo() {
      this.builder = Instance.builder();
    }

    Instance toInstance() {

      if (node == null || privateIp == null) {
        return null;
      }

      return builder
          .node(node)
          .privateIpAddress(privateIp)
          .build();
    }
  }
}
