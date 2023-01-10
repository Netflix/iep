/*
 * Copyright 2014-2023 Netflix, Inc.
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

import com.netflix.iep.config.ConfigManager;
import com.netflix.spectator.nflx.tagging.NetflixTagging;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @deprecated For determining common tags use {@link NetflixTagging} from the
 * {@code spectator-nflx-tagging} library instead. For helpers to access Netflix attributes from
 * the environment use {@link com.netflix.iep.config.NetflixEnvironment} from {@code iep-nflxenv}.
 */
@Deprecated
public class NetflixEnvironment {
  private NetflixEnvironment() {}

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

  /**
   * Extract common infrastructure tags for use with metrics, logs, etc from the
   * Netflix environment variables.
   *
   * @return
   *     Common tags based on the environment.
   */
  public static Map<String, String> commonTags() {
    return NetflixTagging.commonTags();
  }

  /**
   * Extract common infrastructure tags for use with metrics, logs, etc from the
   * Netflix environment variables.
   *
   * @param getenv
   *     Function used to retrieve the value of an environment variable.
   * @return
   *     Common tags based on the environment.
   */
  public static Map<String, String> commonTags(Function<String, String> getenv) {
    return NetflixTagging.commonTags(getenv);
  }

  /**
   * Extract common infrastructure tags for use with Atlas metrics from the
   * Netflix environment variables. This may be a subset of those used for other
   * contexts like logs.
   *
   * @return
   *     Common tags based on the environment.
   */
  public static Map<String, String> commonTagsForAtlas() {
    return NetflixTagging.commonTagsForAtlas();
  }

  /**
   * Extract common infrastructure tags for use with Atlas metrics from the
   * Netflix environment variables. This may be a subset of those used for other
   * contexts like logs.
   *
   * @param getenv
   *     Function used to retrieve the value of an environment variable.
   * @return
   *     Common tags based on the environment.
   */
  public static Map<String, String> commonTagsForAtlas(Function<String, String> getenv) {
    return NetflixTagging.commonTagsForAtlas(getenv);
  }

  /**
   * Extract common infrastructure tags for use with Atlas metrics from the
   * Netflix environment variables. This may be a subset of those used for other
   * contexts like logs.
   *
   * @param getenv
   *     Function used to retrieve the value of an environment variable.
   * @param keyPredicate
   *     Predicate to determine if a common tag key should be included as part of
   *     the Atlas tags.
   * @return
   *     Common tags based on the environment.
   */
  public static Map<String, String> commonTagsForAtlas(
      Function<String, String> getenv, Predicate<String> keyPredicate) {
    return NetflixTagging.commonTagsForAtlas(getenv, keyPredicate);
  }

  /**
   * Returns the recommended predicate to use for filtering out the set of common tags
   * to the set that should be used on Atlas metrics.
   *
   * @param getenv
   *     Function used to retrieve the value of an environment variable.
   * @return
   *     Predicate that evaluates to true if a tag key should be included.
   */
  public static Predicate<String> defaultAtlasKeyPredicate(Function<String, String> getenv) {
    return NetflixTagging.defaultAtlasKeyPredicate(getenv);
  }


  /**
   * Returns the recommended predicate to use for skipping common tags based on the
   * {@code ATLAS_SKIP_COMMON_TAGS} environment variable.
   *
   * @param getenv
   *     Function used to retrieve the value of an environment variable.
   * @return
   *     Predicate that evaluates to true if a tag key should be included.
   */
  public static Predicate<String> atlasSkipTagsPredicate(Function<String, String> getenv) {
    return NetflixTagging.atlasSkipTagsPredicate(getenv);
  }
}
