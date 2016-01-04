/*
 * Copyright 2014-2016 Netflix, Inc.
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

import java.net.InetAddress;

public class NetflixEnvironment {
  private NetflixEnvironment() {}

  private static String LOCAL_APP = "local";

  private static volatile boolean bitmap$0 = false;
  private static String ami;

  private static volatile boolean bitmap$1 = false;
  private static String vmtype;

  private static volatile boolean bitmap$2 = false;
  private static String vpcid;

  private static volatile boolean bitmap$3 = false;
  private static String region;

  private static volatile boolean bitmap$4 = false;
  private static String zone;

  private static volatile boolean bitmap$5 = false;
  private static String instanceId;

  private static volatile boolean bitmap$6 = false;
  private static String app;

  private static volatile boolean bitmap$7 = false;
  private static String cluster;

  private static volatile boolean bitmap$8 = false;
  private static String asg;

  private static volatile boolean bitmap$9 = false;
  private static String stack;

  private static volatile boolean bitmap$10 = false;
  private static String env;

  public static String ami() {
    if (!bitmap$0) {
      synchronized(NetflixEnvironment.class) {
        if (!bitmap$0) {
          ami = System.getenv("EC2_AMI_ID");
          bitmap$0 = true;
        }
      }
    }
    return ami;
  }

  public static String vmtype() {
    if (!bitmap$1) {
      synchronized(NetflixEnvironment.class) {
        if (!bitmap$1) {
          vmtype = System.getenv("EC2_INSTANCE_TYPE");
          bitmap$1 = true;
        }
      }
    }
    return vmtype;
  }

  public static String vpcId() {
    if (!bitmap$2) {
      synchronized(NetflixEnvironment.class) {
        if (!bitmap$2) {
          vpcid = System.getenv("EC2_VPC_ID");
          bitmap$2 = true;
        }
      }
    }
    return vpcid;
  }

  public static String region() {
    if (!bitmap$3) {
      synchronized(NetflixEnvironment.class) {
        if (!bitmap$3) {
          region = System.getenv("EC2_REGION");
          if (region == null) region = "us-nflx-1";
          bitmap$3 = true;
        }
      }
    }
    return region;
  }

  public static String zone() {
    if (!bitmap$4) {
      synchronized(NetflixEnvironment.class) {
        if (!bitmap$4) {
          zone = System.getenv("EC2_AVAILABILITY_ZONE");
          if (zone == null) zone = "us-nflx-1a";
          bitmap$4 = true;
        }
      }
    }
    return zone;
  }

  public static String instanceId() {
    if (!bitmap$5) {
      synchronized(NetflixEnvironment.class) {
        if (!bitmap$5) {
          instanceId = System.getenv("EC2_INSTANCE_ID");
          if (instanceId == null) {
            try { instanceId = InetAddress.getLocalHost().getHostName(); }
            catch (Exception e) { instanceId = "localhost"; }
          }
          bitmap$5 = true;
        }
      }
    }
    return instanceId;
  }

  public static String app() {
    if (!bitmap$6) {
      synchronized(NetflixEnvironment.class) {
        if (!bitmap$6) {
          app = System.getenv("NETFLIX_APP");
          if (app == null) app = LOCAL_APP;
          bitmap$6 = true;
        }
      }
    }
    return app;
  }

  public static String cluster() {
    if (!bitmap$7) {
      synchronized(NetflixEnvironment.class) {
        if (!bitmap$7) {
          cluster = System.getenv("NETFLIX_CLUSTER");
          if (cluster == null) cluster = LOCAL_APP;
          bitmap$7 = true;
        }
      }
    }
    return cluster;
  }

  public static String asg() {
    if (!bitmap$8) {
      synchronized(NetflixEnvironment.class) {
        if (!bitmap$8) {
          asg = System.getenv("NETFLIX_AUTO_SCALE_GROUP");
          if (asg == null) asg = LOCAL_APP;
          bitmap$8 = true;
        }
      }
    }
    return asg;
  }

  public static String stack() {
    if (!bitmap$9) {
      synchronized(NetflixEnvironment.class) {
        if (!bitmap$9) {
          stack = System.getenv("NETFLIX_STACK");
          if (stack == null) stack = "none";
          bitmap$9 = true;
        }
      }
    }
    return stack;
  }

  public static String env() {
    if (!bitmap$10) {
      synchronized(NetflixEnvironment.class) {
        if (!bitmap$10) {
          env = System.getenv("NETFLIX_ENVIRONMENT");
          if (env == null) env = "dev";
          bitmap$10 = true;
        }
      }
    }
    return env;
  }
}
