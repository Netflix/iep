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
package com.netflix.iep.leader.redis;

import com.netflix.iep.leader.api.LeaderDatabase;
import com.netflix.iep.leader.api.LeaderId;
import com.netflix.iep.leader.api.ResourceId;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.util.JedisClusterCRC16;

import java.time.Duration;
import java.util.List;

/**
 * A leader database using Redis Cluster. Resources can map to any slot in the
 * cluster and we attempt to get the leader for that slot. If a leader couldn't be
 * found, the election calls will fail until a leader populates the cache.
 *
 * CAS is implemented via watches and transactions on the keys instead of uploading
 * a LUA script.
 */
public class RedisClusterLeaderDatabase implements LeaderDatabase {

  private static final Logger logger = LoggerFactory.getLogger(
      RedisClusterLeaderDatabase.class);

  private static final String LEADER_CONFIG_PATH_NAME = "iep.leader";
  private final RedisClusterLeaderClient jedis;
  private final Duration maxIdleDuration;
  private final String leaderString;

  protected final SetParams updateParams;
  protected final SetParams leaderParams;

  public RedisClusterLeaderDatabase(
      Config config,
      RedisClusterLeaderClient jedis
  ) {
    this.jedis = jedis;
    maxIdleDuration = config.getDuration(LEADER_CONFIG_PATH_NAME + ".maxIdleDuration");
    updateParams = new SetParams()
        .ex(maxIdleDuration.getSeconds())
        .xx();
    leaderParams = new SetParams()
        .ex(maxIdleDuration.getSeconds())
        .nx();
    leaderString = config.getString(LEADER_CONFIG_PATH_NAME + ".leaderId");
  }

  @Override
  public void initialize() {
    // just a read check that should throw if we can't connect to the redis cluster
    jedis.cluster().get("nosuchkey");
    logger.info("Initializing Redis cluster leader");
  }

  @Override
  public LeaderId getLeaderFor(ResourceId resourceId) {
    try {
      String key = getKey(resourceId);
      String data = jedis.cluster().get(key);
      if (data == null) {
        return LeaderId.NO_LEADER;
      } else {
        return LeaderId.create(data);
      }
    } catch (Exception ex) {
      logger.warn("Failed to get row from Redis", ex);
      return LeaderId.UNKNOWN;
    }
  }

  @Override
  public boolean updateLeadershipFor(ResourceId resourceId) {
    // note slight race here where the follower could overtake the leader if it fails. In that
    // case, the watch and transaction should fail and we'd return a false.
    String key = getKey(resourceId);
    int slot = JedisClusterCRC16.getSlot(key);
    try {
      try (Jedis client = jedis.leaderForSlot(slot)) {
        String watchResult = client.watch(key);
        if (watchResult == null || !watchResult.equals("OK")) {
          logger.warn("Invalid watch response: {}", watchResult);
          return false;
        }

        try {
          String data = client.get(key);
          if (data != null && data.equals(leaderString)) {
            Transaction transaction = client.multi();
            transaction.set(key, leaderString, updateParams);
            List<Object> results = transaction.exec();
            if (results != null &&
                results.size() > 0 &&
                results.get(0) != null &&
                results.get(0).equals("OK")) {
              data = client.get(key);
              if (data == null || !data.equals(leaderString)) {
                logger.warn("Successfully executed renewal transaction but leader " +
                    "was still {}", data);
                return false;
              } else {
                logger.debug("Updated leader key {}", key);
                return true;
              }
            } else {
              // fall-through
              logger.debug("Unable to update our leader status. Trying to capture again.");
            }
          }

          return tryToAcquire(client, key);

        } catch (Exception ex) {
          logger.error("Unexpected exception updating leadership key: {}", key, ex);
          return false;

        } finally {
          // transactions unset the watch but we also have a get() call before the
          // transaction so we need to make sure to release that watch just in case
          // something goes wrong between the get parsing and transaction execution.
          String unwatchResult = client.unwatch();
          if (unwatchResult == null || !unwatchResult.equals("OK")) {
            logger.warn("Failure unwatching: {}", unwatchResult);
          }
        }
      }
    } catch (Exception ex) {
      logger.error("Unexpected exception updating leadership key: {}", key, ex);
      return false;
    }
  }

  @Override
  public boolean removeLeadershipFor(ResourceId resourceId) {
    // note slight race here where the follower could overtake the leader if it fails. In that
    // case, the watch and transaction should fail and we'd return a false.
    String key = getKey(resourceId);
    int slot = JedisClusterCRC16.getSlot(key);
    try {
      try (Jedis client = jedis.leaderForSlot(slot)) {
        String watchResult = client.watch(key);
        if (watchResult == null || !watchResult.equals("OK")) {
          logger.warn("Invalid watch response: {}", watchResult);
          return false;
        }

        try {
          // now we check for leadership since we're on a watch
          String data = client.get(key);
          if (data == null || !data.equals(leaderString)) {
            // lost the race but hey, still won.
            logger.warn("Tried to remove leadership but it was already set to {}", data);
            return true;
          } else {
            Transaction transaction = client.multi();
            transaction.del(key);
            List<Object> results = transaction.exec();
            if (results == null ||
                results.isEmpty() ||
                results.get(0) == null ||
                ((long) results.get(0)) < 1) {
              logger.error("Failed to delete the leader key for {}", key);
              return false;
            } else {
              data = client.get(key);
              if (data != null) {
                logger.error("Tried to remove ourselves as leader for {} but it was still marked for {}", key, data);
                return false;
              } else {
                logger.info("Removed ourselves as leader for key {}", key);
                return true;
              }
            }
          }
        } catch (Exception ex) {
          logger.error("Failed to delete key {}", key, ex);
          return false;
        } finally {
          // transactions unset the watch but we also have a get() call before the
          // transaction so we need to make sure to release that watch just in case
          // something goes wrong between the get parsing and transaction execution.
          String unwatchResult = client.unwatch();
          if (unwatchResult == null || !unwatchResult.equals("OK")) {
            logger.warn("Failure unwatching: {}", unwatchResult);
          }
        }
      }
    } catch (Exception ex) {
      logger.error("Unexpected exception removing leader key: {}", key, ex);
      return false;
    }
  }

  // NOTE: We assume the Watch is held here. Just breaking out for readability.
  private boolean tryToAcquire(Jedis client, String key) {
    Transaction transaction = client.multi();
    transaction.set(key, leaderString, leaderParams);
    List<Object> results = transaction.exec();
    if (results == null || results.isEmpty()) {
      return false;
    } else if (results.get(0) == null || !results.get(0).equals("OK")) {
      logger.error("Failed to acquire the leader key for {}", key);
      return false;
    } else {
      String data = client.get(key);
      if (data == null || !data.equals(leaderString)) {
        logger.warn("Successfully executed acquisition transaction but leader was still {}", data);
        return false;
      } else {
        logger.info("Successfully captured leadership for key {}", key);
        return true;
      }
    }
  }

  private String getKey(ResourceId resourceId) {
    // could be more efficient but elections generally only happen every few seconds.
    String id = resourceId.getId();
    return "iep.leader:{" + id + "}";
  }
}
