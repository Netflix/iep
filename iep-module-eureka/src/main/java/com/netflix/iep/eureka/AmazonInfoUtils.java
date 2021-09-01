/*
 * Copyright 2014-2021 Netflix, Inc.
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
package com.netflix.iep.eureka;

import com.netflix.appinfo.AmazonInfo;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

class AmazonInfoUtils {

  private static boolean notNullOrEmpty(String v) {
    return !(v == null || v.isEmpty());
  }

  static AmazonInfo createFromEnvironment() {
    return createFromEnvironment(System::getenv);
  }

  static AmazonInfo createFromEnvironment(Function<String, String> getenv) {
    AmazonInfo info = new AmazonInfo();
    info.setMetadata(createMetadata(getenv));
    return info;
  }

  static Map<String, String> createMetadata(Function<String, String> getenv) {
    Map<String, String> metadata = new TreeMap<>();
    put(metadata, AmazonInfo.MetaDataKey.instanceId, "NETFLIX_INSTANCE_ID", getenv);
    put(metadata, AmazonInfo.MetaDataKey.localIpv4, "EC2_LOCAL_IPV4", getenv);
    put(metadata, AmazonInfo.MetaDataKey.localHostname, "EC2_LOCAL_IPV4", getenv);
    put(metadata, AmazonInfo.MetaDataKey.availabilityZone, "EC2_AVAILABILITY_ZONE", getenv);
    put(metadata, AmazonInfo.MetaDataKey.publicIpv4, "EC2_PUBLIC_IPV4", getenv);
    put(metadata, AmazonInfo.MetaDataKey.publicHostname, "EC2_PUBLIC_IPV4", getenv);
    put(metadata, AmazonInfo.MetaDataKey.ipv6, "NETFLIX_IPV6", getenv);
    put(metadata, AmazonInfo.MetaDataKey.accountId, "NETFLIX_ACCOUNT_ID", getenv);

    if (notNullOrEmpty(getenv.apply("TITUS_TASK_INSTANCE_ID"))) {
      // Titus
      put(metadata, AmazonInfo.MetaDataKey.amiId, "EC2_AMI_ID", getenv);
      metadata.put(AmazonInfo.MetaDataKey.instanceType.toString(), "Titus");
    } else {
      // EC2
      put(metadata, AmazonInfo.MetaDataKey.amiId, "EC2_AMI_ID", getenv);
      put(metadata, AmazonInfo.MetaDataKey.instanceType, "EC2_INSTANCE_TYPE", getenv);
      put(metadata, AmazonInfo.MetaDataKey.mac, "EC2_MAC", getenv);
      put(metadata, AmazonInfo.MetaDataKey.vpcId, "EC2_VPC_ID", getenv);
    }

    return metadata;
  }

  private static void put(
      Map<String, String> m,
      AmazonInfo.MetaDataKey key,
      String envKey,
      Function<String, String> getenv) {
    String v = getenv.apply(envKey);
    if (notNullOrEmpty(v)) {
      m.put(key.toString(), v);
    }
  }
}
