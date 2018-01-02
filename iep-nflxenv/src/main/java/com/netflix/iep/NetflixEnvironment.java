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
package com.netflix.iep;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class NetflixEnvironment {
  private NetflixEnvironment() {}

  private static final String NAMESPACE = "netflix.iep.env.";

  private static final Config CONFIG = ConfigFactory.load();

  public static String ami() {
    return CONFIG.getString(NAMESPACE + "ami");
  }

  public static String vmtype() {
    return CONFIG.getString(NAMESPACE + "vmtype");
  }

  public static String vpcId() {
    return CONFIG.getString(NAMESPACE + "vpc-id");
  }

  public static String region() {
    return CONFIG.getString(NAMESPACE + "region");
  }

  public static String zone() {
    return CONFIG.getString(NAMESPACE + "zone");
  }

  public static String instanceId() {
    return CONFIG.getString(NAMESPACE + "instance-id");
  }

  public static String app() {
    return CONFIG.getString(NAMESPACE + "app");
  }

  public static String cluster() {
    return CONFIG.getString(NAMESPACE + "cluster");
  }

  public static String asg() {
    return CONFIG.getString(NAMESPACE + "asg");
  }

  public static String stack() {
    return CONFIG.getString(NAMESPACE + "stack");
  }

  public static String env() {
    return CONFIG.getString(NAMESPACE + "environment");
  }

  public static String accountId() {
    return CONFIG.getString(NAMESPACE + "account-id");
  }

  public static String accountName() {
    return CONFIG.getString(NAMESPACE + "account");
  }

  public static String accountType() {
    return CONFIG.getString(NAMESPACE + "account-type");
  }

  public static String accountEnv() {
    return CONFIG.getString(NAMESPACE + "account-env");
  }
}
