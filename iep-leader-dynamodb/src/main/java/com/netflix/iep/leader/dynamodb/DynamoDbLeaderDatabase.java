/*
 * Copyright 2014-2022 Netflix, Inc.
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
package com.netflix.iep.leader.dynamodb;

import com.google.common.collect.Lists;
import com.netflix.iep.leader.api.LeaderDatabase;
import com.netflix.iep.leader.api.LeaderId;
import com.netflix.iep.leader.api.ResourceId;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TableStatus;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Singleton
public class DynamoDbLeaderDatabase implements LeaderDatabase {

  public static class TableActiveTimeoutException extends RuntimeException {
    TableActiveTimeoutException(String message) {
      super(message);
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(DynamoDbLeaderDatabase.class);

  private static final String LEADER_CONFIG_PATH_NAME = "iep.leader";
  private static final String DB_CONFIG_PATH_NAME = LEADER_CONFIG_PATH_NAME + ".dynamodb";

  private static final String LEADER_ID_PLACEHOLDER = ":leaderId";
  private static final String LEADER_TIMEOUT_PLACEHOLDER = ":timeoutMillis";
  private static final String NO_LEADER_PLACEHOLDER = ":noLeader";
  private static final String NOW_MILLIS_PLACEHOLDER = ":nowMillis";

  // update the record if it doesn't have the leaderId attribute
  // or the leader ID is my ID
  // or the timeout threshold has been reached
  private static final String LEADER_UPDATE_CONDITION_FORMAT =
      "attribute_not_exists(%s) OR %1$s = %s OR %s <= %s";
  private static final String LEADER_UPDATE_FORMAT = "SET %s = %s, %s = %s";
  private static final String LEADER_REMOVE_FORMAT = "SET %s = %s";
  private static final String LEADER_REMOVE_CONDITION = "%s = %s";

  private final DynamoDbClient db;
  private final Config config;
  private final LeaderId leaderId;
  private final String tableName;
  private final String hashKeyName;
  private final Long readCapacityUnits;
  private final Long writeCapacityUnits;
  private final Duration tableActiveTimeout;
  private final String leaderIdAttributeName;
  private final String lastUpdateAttributeName;

  private final String leaderUpdateExpression;
  private final String leaderUpdateConditionExpression;
  private final String removeLeadershipExpression;
  private final String removeLeadershipConditionExpression;

  @Inject
  public DynamoDbLeaderDatabase(DynamoDbClient db, Config config) {
    this(
        db,
        config,
        LeaderId.create(config),
        config.getString(DB_CONFIG_PATH_NAME + ".tableName"),
        config.getString(DB_CONFIG_PATH_NAME + ".tableHashKeyName"),
        config.getLong(DB_CONFIG_PATH_NAME + ".tableReadCapacityUnits"),
        config.getLong(DB_CONFIG_PATH_NAME + ".tableWriteCapacityUnits"),
        config.getDuration(DB_CONFIG_PATH_NAME + ".tableActiveTimeout"),
        config.getString(DB_CONFIG_PATH_NAME + ".leaderIdAttributeName"),
        config.getString(DB_CONFIG_PATH_NAME + ".lastUpdateAttributeName")
    );
  }

  public DynamoDbLeaderDatabase(
      DynamoDbClient db,
      Config config,
      LeaderId leaderId,
      String tableName,
      String hashKeyName,
      Long readCapacityUnits,
      Long writeCapacityUnits,
      Duration tableActiveTimeout,
      String leaderIdAttributeName,
      String lastUpdateAttributeName
  ) {
    Objects.requireNonNull(db, "db");
    Objects.requireNonNull(config, "config");
    Objects.requireNonNull(leaderId, "leaderId");
    Objects.requireNonNull(tableName, "tableName");
    Objects.requireNonNull(hashKeyName, "hashKeyName");
    Objects.requireNonNull(readCapacityUnits, "readCapacityUnits");
    Objects.requireNonNull(writeCapacityUnits, "writeCapacityUnits");
    Objects.requireNonNull(tableActiveTimeout, "tableActiveTimeout");
    Objects.requireNonNull(leaderIdAttributeName, "leaderIdAttributeName");
    Objects.requireNonNull(lastUpdateAttributeName, "lastUpdateAttributeName");

    this.db = db;
    this.config = config;
    this.leaderId = leaderId;
    this.tableName = tableName;
    this.hashKeyName = hashKeyName;
    this.readCapacityUnits = readCapacityUnits;
    this.writeCapacityUnits = writeCapacityUnits;
    this.tableActiveTimeout = tableActiveTimeout;
    this.leaderIdAttributeName = leaderIdAttributeName;
    this.lastUpdateAttributeName = lastUpdateAttributeName;

    this.leaderUpdateExpression =
        formatUpdateLeader(LEADER_UPDATE_FORMAT, NOW_MILLIS_PLACEHOLDER);
    this.leaderUpdateConditionExpression =
        formatUpdateLeader(LEADER_UPDATE_CONDITION_FORMAT, LEADER_TIMEOUT_PLACEHOLDER);
    this.removeLeadershipExpression =
        formatRemoveLeader(LEADER_REMOVE_FORMAT, NO_LEADER_PLACEHOLDER);
    this.removeLeadershipConditionExpression =
        formatRemoveLeader(LEADER_REMOVE_CONDITION, LEADER_ID_PLACEHOLDER);
  }

  private String formatUpdateLeader(String formatString, String epochMillisPlaceholder) {
    return String.format(formatString,
        leaderIdAttributeName,
        LEADER_ID_PLACEHOLDER,
        lastUpdateAttributeName,
        epochMillisPlaceholder
    );
  }

  private String formatRemoveLeader(String formatString, String leaderPlaceholder) {
    return String.format(formatString, leaderIdAttributeName, leaderPlaceholder);
  }

  @Override
  public void initialize() {
    final Collection<AttributeDefinition> attributeDefinitions = Lists.newArrayList(
        AttributeDefinition
            .builder()
            .attributeName(hashKeyName)
            .attributeType(ScalarAttributeType.S)
            .build()
    );

    final Collection<KeySchemaElement> keySchemaElements = Lists.newArrayList(
        KeySchemaElement
            .builder()
            .attributeName(hashKeyName)
            .keyType(KeyType.HASH)
            .build()
    );

    final ProvisionedThroughput provisionedThroughput =
        ProvisionedThroughput
            .builder()
            .readCapacityUnits(readCapacityUnits)
            .writeCapacityUnits(writeCapacityUnits)
            .build();

    final CreateTableRequest createTableRequest =
        CreateTableRequest
            .builder()
            .tableName(tableName)
            .keySchema(keySchemaElements)
            .attributeDefinitions(attributeDefinitions)
            .provisionedThroughput(provisionedThroughput)
            .build();

    try {
      CreateTableResponse tableResult = db.createTable(createTableRequest);
      logger.info("Created table '{}': Table Status = {}, SDK HTTP Response = {}",
          tableName,
          tableResult.tableDescription().tableStatusAsString(),
          tableResult.sdkHttpResponse().statusCode()
      );
    } catch (ResourceInUseException e) {
      logger.debug("Did not create table '{}'; it already exists", tableName);
    }

    waitUntilTableIsActive(db, tableName, tableActiveTimeout);
  }

  @Override
  public LeaderId getLeaderFor(ResourceId resourceId) {
    final String resourceIdStr = resourceId.getId();
    final Map<String, AttributeValue> resourceRecordKey = Collections.singletonMap(
        hashKeyName, AttributeValue.builder().s(resourceIdStr).build()
    );

    final GetItemRequest request =
        GetItemRequest
            .builder()
            .tableName(tableName)
            .key(resourceRecordKey)
            .projectionExpression(leaderIdAttributeName)
            .consistentRead(true)
            .build();

    LeaderId leader = LeaderId.NO_LEADER;
    try {
      final Map<String, AttributeValue> itemMap = db.getItem(request).item();

      if (itemMap != null) {
        final AttributeValue leaderIdAttrVal = itemMap.get(leaderIdAttributeName);
        if (leaderIdAttrVal != null) {
          leader = LeaderId.create(leaderIdAttrVal.s());
        }
      }
    } catch (ResourceNotFoundException e) {
      logger.warn("No current record for resource " + resourceIdStr, e);
    }
    return leader;
  }

  @Override
  public boolean updateLeadershipFor(ResourceId resource) {
    final String resourceId = resource.getId();

    boolean updated;
    try {
      final Instant now = Instant.now();
      final Duration leaderMaxIdleDuration =
          config.getDuration(LEADER_CONFIG_PATH_NAME + ".maxIdleDuration");
      final Instant leaderTimeoutInstant = now.minus(leaderMaxIdleDuration);

      final Map<String, AttributeValue> resourceRecordKey = Collections.singletonMap(
          hashKeyName, AttributeValue.builder().s(resourceId).build()
      );

      final Map<String, AttributeValue> expressionAttributeValues = new HashMap<>(3, 1.0f);
      expressionAttributeValues.put(LEADER_ID_PLACEHOLDER,
          AttributeValue.builder().s(leaderId.getId()).build()
      );
      expressionAttributeValues.put(NOW_MILLIS_PLACEHOLDER,
          AttributeValue.builder().n(String.valueOf(now.toEpochMilli())).build()
      );
      expressionAttributeValues.put(LEADER_TIMEOUT_PLACEHOLDER,
          AttributeValue.builder().n(String.valueOf(leaderTimeoutInstant.toEpochMilli())).build()
      );

      final UpdateItemRequest updateRequest =
          UpdateItemRequest
              .builder()
              .tableName(tableName)
              .key(resourceRecordKey)
              .updateExpression(leaderUpdateExpression)
              .conditionExpression(leaderUpdateConditionExpression)
              .expressionAttributeValues(expressionAttributeValues)
              .build();

      updated = db.updateItem(updateRequest).sdkHttpResponse().isSuccessful();
    } catch (ConditionalCheckFailedException e) {
      logger.debug("There is already an active leader for resource: {}", resourceId);
      updated = false;
    }

    return updated;
  }

  @Override
  public boolean removeLeadershipFor(ResourceId resourceId) {
    boolean removed;
    try {
      final Map<String, AttributeValue> resourceRecordKey = Collections.singletonMap(
          hashKeyName, AttributeValue.builder().s(resourceId.getId()).build()
      );

      final Map<String, AttributeValue> expressionAttributeValues = new HashMap<>(2, 1.0f);
      expressionAttributeValues.put(LEADER_ID_PLACEHOLDER,
          AttributeValue.builder().s(leaderId.getId()).build()
      );
      expressionAttributeValues.put(NO_LEADER_PLACEHOLDER,
          AttributeValue.builder().s(LeaderId.NO_LEADER.getId()).build()
      );

      final UpdateItemRequest updateRequest = UpdateItemRequest.builder()
          .tableName(tableName)
          .key(resourceRecordKey)
          .updateExpression(removeLeadershipExpression)
          .conditionExpression(removeLeadershipConditionExpression)
          .expressionAttributeValues(expressionAttributeValues)
          .build();

      removed = db.updateItem(updateRequest).sdkHttpResponse().isSuccessful();
    } catch (ConditionalCheckFailedException e) {
      logger.debug(
          "Didn't remove leader attribute for {}; this {} is not its leader.",
          resourceId,
          leaderId.getId());
      removed = false;
    }

    return removed;
  }

  private void waitUntilTableIsActive(
      DynamoDbClient db,
      String tableName,
      Duration tableActiveTimeout
  ) {
    final Instant timeoutInstant = Instant.now().plus(tableActiveTimeout);
    final Duration waitDuration = Duration.ofSeconds(20L);
    while (Instant.now().isBefore(timeoutInstant)) {
      logger.debug("Checking if table '{}' is active", tableName);

      if (tableIsActive(db, tableName)) return;

      logger.debug("Table '{}' is not yet active, waiting ${waitDuration}", tableName);
      try {
        Thread.sleep(waitDuration.toMillis());
      } catch (InterruptedException e) {
        logger.warn("Interrupted while waiting for table '{}' to become active", tableName);
        Thread.currentThread().interrupt();
      }
    }

    final String message = String.format(
        "Table '%s' did not transition to active within: %s",
        tableName, tableActiveTimeout.toString()
    );
    logger.error(message);
    throw new TableActiveTimeoutException(message);
  }

  private boolean tableIsActive(DynamoDbClient db, String tableName) {
    boolean isActive;
    try {
      final DescribeTableResponse tableStatusResult =
          db.describeTable(DescribeTableRequest.builder().tableName(tableName).build());
      final TableStatus tableStatus = tableStatusResult.table().tableStatus();
      logger.debug("Table status = {}", tableStatus);
      isActive = tableStatus.equals(TableStatus.ACTIVE);
    } catch (ResourceNotFoundException e) {
      logger.debug("Table '{}' doesn't exist yet... waiting", tableName);
      isActive = false;
    }
    return isActive;
  }
}
