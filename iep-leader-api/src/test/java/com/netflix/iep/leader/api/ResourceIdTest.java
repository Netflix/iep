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
package com.netflix.iep.leader.api;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@RunWith(JUnit4.class)
public class ResourceIdTest {

  @Test
  public void equalsCompliesWithContract() {
    EqualsVerifier.forClass(ResourceId.class)
        .suppress(Warning.NULL_FIELDS)
        .verify();
  }

  @Test
  public void nullIdThrows() {
    assertThatNullPointerException().isThrownBy(() -> new ResourceId(null));
  }

  @Test
  public void emptyIdThrows() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ResourceId(""));
  }

  @Test
  public void resourceIdsConfigIsOptional() {
    final Config c = ConfigFactory.empty("ResourceIdTest.resourceIdsConfigIsOptional");
    final Collection<ResourceId> actualResourceIds = ResourceId.create(c);
    assertThat(actualResourceIds).isEmpty();
  }

  @Test
  public void factoryHandlesEmptyList() {
    final Config c = ConfigFactory.parseString("iep.leader.resourceIds = []");
    final Collection<ResourceId> actualResourceIds = ResourceId.create(c);

    assertThat(actualResourceIds).isEmpty();
  }

  @Test
  public void factoryHandlesSingleValue() {
    final String id1 = "test-id-one";
    final Config c = ConfigFactory.parseString("iep.leader.resourceIds = [" + id1 + "]");

    final Collection<ResourceId> actualResourceIds = ResourceId.create(c);

    assertThat(actualResourceIds).containsExactly(new ResourceId(id1));
  }

  @Test
  public void factoryHandlesMultipleValues() {
    final String id1 = "test-id-one";
    final String id2 = "test-id-two";
    final String id3 = "test-id-three";
    final String idListStr = String.join(",",id1, id2, id3);
    final Config c = ConfigFactory.parseString("iep.leader.resourceIds = [" + idListStr + "]");

    final Collection<ResourceId> actualResourceIds = ResourceId.create(c);

    assertThat(actualResourceIds).containsExactly(
        new ResourceId(id1),
        new ResourceId(id2),
        new ResourceId(id3)
    );
  }
}
