package com.netflix.iep.servergroups;

import com.netflix.spectator.ipc.http.HttpClient;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

final class LoaderUtils {

  private LoaderUtils() {
  }

  static final URI EDDA_URI = URI.create("http://localhost:7101/api/v2/netflix/serverGroups");

  static EddaLoader createEddaLoader(String resource) throws Exception {
    HttpClient client = TestHttpClient.resource(200, resource);
    return new EddaLoader(client, EDDA_URI);
  }

  static final URI EUREKA_URI = URI.create("http://localhost:7101/v2/apps");

  static EurekaLoader createEurekaLoader(String resource, String account) throws Exception {
    HttpClient client = TestHttpClient.resource(200, resource);
    Predicate<String> p = (account == null) ? v -> true : account::equals;
    return new EurekaLoader(client, EUREKA_URI, p);
  }
}
