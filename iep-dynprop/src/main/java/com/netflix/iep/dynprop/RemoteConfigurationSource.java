/*
 * Copyright 2015 Netflix, Inc.
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
package com.netflix.iep.dynprop;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.HashMap;

import org.apache.commons.configuration.AbstractConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.appinfo.AmazonInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.PollResult;
import com.netflix.config.PolledConfigurationSource;

/**
 * A polled configuration source backed remote service.
 */
public class RemoteConfigurationSource implements PolledConfigurationSource {

  private static final Logger logger = LoggerFactory.getLogger(RemoteConfigurationSource.class);

  private static final String USE_IP = "platformservice.niws.client.UseIpAddress";
  private static final String CONNECT_TIMEOUT = "platformservice.niws.client.ConnectTimeout";
  private static final String READ_TIMEOUT = "platformservice.niws.client.ReadTimeout";
  private static final String NUM_RETRIES = "platformservice.niws.client.MaxAutoRetriesNextServer";
  private static final String VIP = "platformservice.niws.client.DeploymentContextBasedVipAddresses";

  private final InstanceInfo info;
  private final DiscoveryClient client;

  public RemoteConfigurationSource(InstanceInfo info, DiscoveryClient client) {
    this.info = info;
    this.client = client;
  }

  private String getAsgName() {
    return info.getASGName();
  }

  private String getInstanceId() {
    return info.getId();
  }

  private String getZone() {
    return ((AmazonInfo) info.getDataCenterInfo()).get(AmazonInfo.MetaDataKey.availabilityZone);
  }

  private List<String> getHostsForVip(String vip, boolean useIp) {
    List<InstanceInfo> instances = client.getInstancesByVipAddress(vip, false);
    List<String> filtered = new ArrayList<>(instances.size());
    for (InstanceInfo info : instances) {
      if (info.getStatus() == InstanceInfo.InstanceStatus.UP) {
        String host = useIp ? info.getIPAddr() : info.getHostName();
        filtered.add(host + ":" + info.getPort());
      }
    }
    Collections.shuffle(filtered);
    return filtered;
  }

  private Properties getProperties() throws IOException {
    AbstractConfiguration config = ConfigurationManager.getConfigInstance();
    String vip = config.getString(VIP, "atlas_archaius-main:7001");
    List<String> hosts = getHostsForVip(vip, config.getBoolean(USE_IP, false));

    int numAttempts = config.getInt(NUM_RETRIES) + 1;
    for (int i = 1; i <= numAttempts; ++i) {
      String host = hosts.get(i % hosts.size());
      String url = "http://" + host + "/api/v1/property"
          + "?asg=" + getAsgName()
          + "&instanceId=" + getInstanceId()
          + "&zone=" + getZone();
      logger.debug("attempt {} of {}, requesting properties from: {}", i, numAttempts, url);

      try {
        URLConnection con = new URL(url).openConnection();
        con.setConnectTimeout(config.getInt(CONNECT_TIMEOUT, 1000));
        con.setReadTimeout(config.getInt(READ_TIMEOUT, 5000));

        Properties props = new Properties();
        try (InputStream in = con.getInputStream()) {
          props.load(in);
        }

        logger.debug("fetched {} properties from: {}", props.size(), url);
        return props;
      } catch (IOException e) {
        String msg = String.format("attempt %d of %d failed, url: %s", i, numAttempts, url);
        if (i == numAttempts) {
          logger.error(msg, e);
          throw e;
        } else {
          logger.warn(msg, e);
        }
      }
    }

    // Shouldn't get here
    throw new IllegalStateException("failed to get properties");
  }

  @Override
  public PollResult poll(boolean initial, Object checkPoint) throws IOException {

    Properties props = getProperties();
    HashMap<String,Object> mProps = new HashMap<>();
    for (String key : props.stringPropertyNames())
      mProps.put(key, props.getProperty(key));

    PollResult pollResult = PollResult.createFull(mProps);
    if (initial) {
      logger.info("initialized dynamic properties");
    }
    return pollResult;
  }
}
