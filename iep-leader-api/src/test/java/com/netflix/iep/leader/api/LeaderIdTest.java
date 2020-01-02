/*
 * Copyright 2014-2020 Netflix, Inc.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@RunWith(JUnit4.class)
public class LeaderIdTest {

  @Test
  public void equalsCompliesWithContract() {
    EqualsVerifier.forClass(LeaderId.class)
        .suppress(Warning.NULL_FIELDS)
        .verify();
  }

  @Test
  public void nullIdThrows() {
    assertThatNullPointerException().isThrownBy(() -> LeaderId.create((String) null));
  }

  @Test
  public void emptyIdThrows() {
    assertThatIllegalArgumentException().isThrownBy(() -> LeaderId.create(""));
  }

  @Test
  public void factoryUsesValueInConfig() {
    final String id = "test-id";
    final Config c = ConfigFactory.parseString("iep.leader.leaderId = " + id);
    final LeaderId leaderId = LeaderId.create(c);

    assertThat(leaderId).isEqualTo(LeaderId.create(id));
  }
}
