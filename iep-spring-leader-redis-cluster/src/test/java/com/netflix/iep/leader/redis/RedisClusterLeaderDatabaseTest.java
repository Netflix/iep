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

import com.netflix.iep.leader.api.LeaderId;
import com.netflix.iep.leader.api.ResourceId;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import redis.clients.jedis.Connection;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class RedisClusterLeaderDatabaseTest {

  private final Config config = getConfig(Collections.emptyMap());
  private final String key = "iep.leader:{MyCluster}";
  private final ResourceId resourceId = new ResourceId("MyCluster");
  private final String leader = "UT";

  private RedisClusterLeaderClient cluster;
  private RedisClusterLeaderDatabase db;
  private Jedis client;
  private Connection conn;
  private Transaction transaction;

  @Before
  public void before() {
    cluster = mock(RedisClusterLeaderClient.class);
    JedisCluster mockCluster = mock(JedisCluster.class);
    when(cluster.cluster()).thenReturn(mockCluster);
    db = new RedisClusterLeaderDatabase(config, cluster);
    client = mock(Jedis.class);
    when(cluster.leaderForSlot(anyInt())).thenReturn(client);
    conn = mock(Connection.class);
    transaction = spy(new Transaction(conn, true));
    when(client.multi()).thenAnswer((Answer<Transaction>) invocation -> {
      transaction.multi();
      return transaction;
    });
  }

  @Test
  public void initialize() {
    db.initialize();
  }

  @Test(expected = JedisConnectionException.class)
  public void initializeFailure() {
    when(cluster.cluster().get(anyString())).thenThrow(new JedisConnectionException("UT"));
    db.initialize();
  }

  @Test
  public void getLeaderNoKey() {
    when(cluster.cluster().get(key)).thenReturn(null);
    Assert.assertEquals(LeaderId.NO_LEADER, db.getLeaderFor(resourceId));
  }

  @Test
  public void getLeaderSet() {
    when(cluster.cluster().get(key)).thenReturn(leader);
    Assert.assertEquals(LeaderId.create(leader), db.getLeaderFor(resourceId));
  }

  @Test
  public void getLeaderException() {
    when(cluster.cluster().get(key)).thenThrow(new JedisException("UT"));
    Assert.assertEquals(LeaderId.UNKNOWN, db.getLeaderFor(resourceId));
  }

  @Test
  public void updateLeadershipFor_Success() {
    when(client.watch(anyString())).thenReturn("OK");
    when(conn.getObjectMultiBulkReply()).thenReturn(new ArrayList<>(){{
      add("OK".getBytes(StandardCharsets.UTF_8));
    }});
    when(client.get(key)).thenReturn(null)
                         .thenReturn(leader);
    Assert.assertTrue(db.updateLeadershipFor(resourceId));
    verify(transaction, times(1)).set(key, leader, db.leaderParams);
    // make sure we use the transaction, not the client or cluster client.
    verify(cluster.cluster(), never()).set(key, leader, db.leaderParams);
    verify(client, never()).set(key, leader, db.leaderParams);
    verify(client, times(1)).watch(key);
    verify(client, times(1)).unwatch();
  }

  @Test
  public void updateLeadershipFor_AcquireReadBackFailed() {
    when(client.watch(anyString())).thenReturn("OK");
    when(conn.getObjectMultiBulkReply()).thenReturn(new ArrayList<>(){{
      add("OK".getBytes(StandardCharsets.UTF_8));
    }});
    when(client.get(key)).thenReturn(null)
                         .thenReturn("SomethingElse");
    Assert.assertFalse(db.updateLeadershipFor(resourceId));
  }

  @Test
  public void updateLeadershipFor_AcquireTransactionFailedNull() {
    when(client.watch(anyString())).thenReturn("OK");
    when(conn.getObjectMultiBulkReply()).thenReturn(new ArrayList<>(){{
      add(null);
    }});
    Assert.assertFalse(db.updateLeadershipFor(resourceId));
    verify(client, times(1)).unwatch();
  }

  @Test
  public void updateLeadershipFor_AcquireTransactionFailedInvalid() {
    when(client.watch(anyString())).thenReturn("OK");
    when(conn.getObjectMultiBulkReply()).thenReturn(new ArrayList<>(){{
      add("Invalid".getBytes(StandardCharsets.UTF_8));
    }});
    Assert.assertFalse(db.updateLeadershipFor(resourceId));
    verify(client, times(1)).unwatch();
  }

  @Test
  public void updateLeadershipFor_AcquireTransactionException() {
    when(client.watch(anyString())).thenReturn("OK");
    when(conn.getObjectMultiBulkReply()).thenThrow(new JedisException("UT"));
    Assert.assertFalse(db.updateLeadershipFor(resourceId));
    verify(client, times(1)).unwatch();
  }

  @Test
  public void updateLeadershipFor_Renew() {
    when(client.watch(anyString())).thenReturn("OK");
    when(conn.getObjectMultiBulkReply()).thenReturn(new ArrayList<>(){{
      add("OK".getBytes(StandardCharsets.UTF_8));
    }});
    when(client.get(key)).thenReturn(leader)
                         .thenReturn(leader);
    Assert.assertTrue(db.updateLeadershipFor(resourceId));
    verify(transaction, times(1)).set(key, leader, db.updateParams);
    // make sure we use the transaction, not the client or cluster client.
    verify(cluster.cluster(), never()).set(key, leader, db.updateParams);
    verify(client, never()).set(key, leader, db.updateParams);
    verify(client, times(1)).watch(key);
    verify(client, times(1)).unwatch();
  }

  @Test
  public void updateLeadershipFor_RenewReadBackFailed() {
    when(client.watch(anyString())).thenReturn("OK");
    when(conn.getObjectMultiBulkReply()).thenReturn(new ArrayList<>(){{
      add("OK".getBytes(StandardCharsets.UTF_8));
    }});
    when(client.get(key)).thenReturn(leader)
                         .thenReturn("Other");
    Assert.assertFalse(db.updateLeadershipFor(resourceId));
    verify(transaction, times(1)).set(key, leader, db.updateParams);
  }

  @Test
  public void updateLeadershipFor_RenewTransactionFailedNull() {
    when(client.watch(anyString())).thenReturn("OK");
    when(conn.getObjectMultiBulkReply()).thenReturn(new ArrayList<>(){{
      add(null);
    }});
    when(client.get(key)).thenReturn(leader);
    Assert.assertFalse(db.updateLeadershipFor(resourceId));
  }

  @Test
  public void updateLeadershipFor_RenewTransactionFailedInvalid() {
    when(client.watch(anyString())).thenReturn("OK");
    when(conn.getObjectMultiBulkReply()).thenReturn(new ArrayList<>(){{
      add("Invalid");
    }});
    when(client.get(key)).thenReturn(leader);
    Assert.assertFalse(db.updateLeadershipFor(resourceId));
  }

  @Test
  public void updateLeadershipFor_RenewTransactionException() {
    when(client.watch(anyString())).thenReturn("OK");
    when(conn.getObjectMultiBulkReply()).thenThrow(new JedisException("UT"));
    when(client.get(key)).thenReturn(leader);
    Assert.assertFalse(db.updateLeadershipFor(resourceId));
  }

  @Test
  public void updateLeadershipFor_WatchInvalid() {
    when(client.watch(anyString())).thenReturn("Invalid");
    Assert.assertFalse(db.updateLeadershipFor(resourceId));
  }

  @Test
  public void updateLeadershipFor_WatchException() {
    when(client.watch(anyString())).thenThrow(new JedisException("UT"));
    Assert.assertFalse(db.updateLeadershipFor(resourceId));
  }

  @Test
  public void removeLeadershipFor_Success() {
    when(client.watch(anyString())).thenReturn("OK");
    when(conn.getObjectMultiBulkReply()).thenReturn(new ArrayList<>(){{
      add(1L);
    }});
    when(client.get(key)).thenReturn(leader)
                         .thenReturn(null);
    Assert.assertTrue(db.removeLeadershipFor(resourceId));
    verify(transaction, times(1)).del(key);
    // make sure we use the transaction, not the client or cluster client.
    verify(cluster.cluster(), never()).del(key);
    verify(client, never()).del(key);
    verify(client, times(1)).watch(key);
    verify(client, times(1)).unwatch();
  }

  @Test
  public void removeLeadershipFor_ReadBackFailed() {
    when(client.watch(anyString())).thenReturn("OK");
    when(conn.getObjectMultiBulkReply()).thenReturn(new ArrayList<>(){{
      add(1L);
    }});
    when(client.get(key)).thenReturn(leader)
                         .thenReturn(leader);
    Assert.assertFalse(db.removeLeadershipFor(resourceId));
    verify(transaction, times(1)).del(key);
    verify(client, times(1)).unwatch();
  }

  @Test
  public void removeLeadershipFor_TransactionFailed() {
    when(client.watch(anyString())).thenReturn("OK");
    when(conn.getObjectMultiBulkReply()).thenReturn(new ArrayList<>(){{
      add(0L);
    }});
    when(client.get(key)).thenReturn(leader);
    Assert.assertFalse(db.removeLeadershipFor(resourceId));
    verify(transaction, times(1)).del(key);
    verify(client, times(1)).unwatch();
  }

  @Test
  public void removeLeadershipFor_TransactionException() {
    when(client.watch(anyString())).thenReturn("OK");
    when(conn.getObjectMultiBulkReply()).thenThrow(new JedisException("UT"));
    when(client.get(key)).thenReturn(leader);
    Assert.assertFalse(db.removeLeadershipFor(resourceId));
    verify(transaction, times(1)).del(key);
    verify(client, times(1)).unwatch();
  }

  @Test
  public void removeLeadershipFor_AlreadyDeleted() {
    when(client.watch(anyString())).thenReturn("OK");
    when(client.get(key)).thenReturn(null);
    Assert.assertTrue(db.removeLeadershipFor(resourceId));
    verify(transaction, never()).del(key);
    verify(client, times(1)).unwatch();
  }

  @Test
  public void removeLeadershipFor_ClaimedByOther() {
    when(client.watch(anyString())).thenReturn("OK");
    when(client.get(key)).thenReturn("Other");
    Assert.assertTrue(db.removeLeadershipFor(resourceId));
    verify(transaction, never()).del(key);
    verify(client, times(1)).unwatch();
  }

  @Test
  public void removeLeadershipFor_WatchInvalid() {
    when(client.watch(anyString())).thenReturn("Invalid");
    Assert.assertFalse(db.removeLeadershipFor(resourceId));
  }

  @Test
  public void removeLeadershipFor_WatchException() {
    when(client.watch(anyString())).thenThrow(new JedisException("UT"));
    Assert.assertFalse(db.removeLeadershipFor(resourceId));
  }

  private Config getConfig(Map<String, Object> overrides) {
    Map<String, Object> map = new HashMap<>() {{
      put("iep.leader.maxIdleDuration", "15s");
      put("iep.leader.connection.pool.max", 1);
      put("iep.leader.connection.port", 7101);
      put("iep.leader.cmd.timeout", "2s");
      put("iep.leader.leaderId", "UT");
      put("iep.leader.resourceIds", new ArrayList<>(){{
        add("MyCluster");
      }});
    }};
    map.putAll(overrides);
    return ConfigFactory.parseMap(map);
  }
}
