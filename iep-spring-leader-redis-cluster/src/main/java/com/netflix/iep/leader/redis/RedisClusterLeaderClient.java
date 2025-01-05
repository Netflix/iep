/*
 * Copyright 2014-2025 Netflix, Inc.
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
package com.netflix.iep.leader.redis;

import com.typesafe.config.Config;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.executors.ClusterCommandExecutor;
import redis.clients.jedis.executors.CommandExecutor;

import java.time.temporal.ChronoUnit;

/**
 * Shim used for unit testing and for differentiating the cluster client for
 * leader election from other uses.
 */
public class RedisClusterLeaderClient {

  private static final Logger logger = LoggerFactory.getLogger(
      RedisClusterLeaderClient.class);

  private static final String CONFIG_PATH_NAME = "iep.leader.rediscluster";

  private final WrappedJedis jedis;

  public RedisClusterLeaderClient(Config config) {
    GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<Connection>();
    poolConfig.setMaxTotal(config.getInt(CONFIG_PATH_NAME + ".connection.pool.max"));
    String uri = config.getString(CONFIG_PATH_NAME + ".uri");
    logger.info("Using Redis Cluster {} for leader election", uri);
    HostAndPort hap = new HostAndPort(uri,
        config.getInt(CONFIG_PATH_NAME + ".connection.port"));
    jedis = new WrappedJedis(
        hap,
        (int) config.getDuration(CONFIG_PATH_NAME + ".cmd.timeout").getSeconds() * 1000,
        poolConfig
    );
  }

  public JedisCluster cluster() {
    return jedis;
  }

  /**
   * Attempts to get a connection to the leader for a slot from the pool to be used
   * in transactions. It's useless to execute a transaction on a follower.
   * Jedis doesn't have a great way to assure we're getting the leader from the
   * cache for a connection pool so we double-check that with an INFO call. If
   * the info call indicates the connection is for a follower, we'll force refresh
   * the cache and try one more time. If that still fails, we throw an exception.
   *
   * Package private for unit testing.
   *
   * @param slot
   *     The slot to fetch the leader connection from.
   * @return
   *     The lead slot client or an exception if no leader is found.
   */
  Jedis leaderForSlot(int slot) {
    Jedis client = getLeaderForSlot(slot);
    if (client == null) {
      // force renewal and then try one more time.
      if (jedis.getCommandExecutor() instanceof ClusterCommandExecutor) {
        ((ClusterCommandExecutor) jedis.getCommandExecutor()).provider.renewSlotCache();
      }

      client = getLeaderForSlot(slot);
    }

    if (client != null) {
      return client;
    }
    throw new JedisException("Unable to find leader for slot " + slot
        + " after two attempts.");
  }

  private Jedis getLeaderForSlot(int slot) {
    Connection conn = jedis.getConnectionFromSlot(slot);
    conn.sendCommand(Protocol.Command.INFO, "Replication");
    String info = conn.getBulkReply();
    if (info.contains("role:master")) {
      return new Jedis(conn);
    }
    logger.warn("Received connection to {} for slot {} but it was not the leader",
        conn, slot);
    return null;
  }

  /**
   * Wrapping here to avoid reflection. This way we'll get a compile error if they
   * change out the CommandExecutor under the hood.
   */
  private class WrappedJedis extends JedisCluster {

    public WrappedJedis(
        HostAndPort node,
        int timeout,
        GenericObjectPoolConfig<Connection> poolConfig
    ) {
      super(node, timeout, poolConfig);
    }

    CommandExecutor getCommandExecutor() {
      return executor;
    }
  }
}
