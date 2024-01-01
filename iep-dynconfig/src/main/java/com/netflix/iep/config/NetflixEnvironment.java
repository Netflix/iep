/*
 * Copyright 2014-2024 Netflix, Inc.
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
package com.netflix.iep.config;

public class NetflixEnvironment {
  private NetflixEnvironment() {
  }

  private static final String NAMESPACE = "netflix.iep.env.";

  public static String ami() {
    return ConfigManager.get().getString(NAMESPACE + "ami");
  }

  public static String vmtype() {
    return ConfigManager.get().getString(NAMESPACE + "vmtype");
  }

  public static String vpcId() {
    return ConfigManager.get().getString(NAMESPACE + "vpc-id");
  }

  public static String region() {
    return ConfigManager.get().getString(NAMESPACE + "region");
  }

  public static String zone() {
    return ConfigManager.get().getString(NAMESPACE + "zone");
  }

  public static String instanceId() {
    return ConfigManager.get().getString(NAMESPACE + "instance-id");
  }

  public static String app() {
    return ConfigManager.get().getString(NAMESPACE + "app");
  }

  public static String cluster() {
    return ConfigManager.get().getString(NAMESPACE + "cluster");
  }

  public static String asg() {
    return ConfigManager.get().getString(NAMESPACE + "asg");
  }

  public static String stack() {
    return ConfigManager.get().getString(NAMESPACE + "stack");
  }

  public static String env() {
    return ConfigManager.get().getString(NAMESPACE + "environment");
  }

  public static String accountId() {
    return ConfigManager.get().getString(NAMESPACE + "account-id");
  }

  public static String accountName() {
    return ConfigManager.get().getString(NAMESPACE + "account");
  }

  public static String accountType() {
    return ConfigManager.get().getString(NAMESPACE + "account-type");
  }

  public static String accountEnv() {
    return ConfigManager.get().getString(NAMESPACE + "account-env");
  }

  public static String insightAccountId() {
    return ConfigManager.get().getString(NAMESPACE + "insight-account-id");
  }

  public static String detail() {
    return ConfigManager.get().getString(NAMESPACE + "detail");
  }
}
