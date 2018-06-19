/*
 * Copyright 2014-2018 Netflix, Inc.
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
package com.netflix.iep.admin.endpoints;

import com.netflix.spectator.api.DefaultRegistry;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.patterns.PolledMeter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@RunWith(JUnit4.class)
public class SpectatorEndpointTest {

  private final Registry registry = new DefaultRegistry();
  private final SpectatorEndpoint endpoint = new SpectatorEndpoint(registry);

  public SpectatorEndpointTest() {
    registry.counter("counter1", "a", "1", "b", "2").increment();
    registry.counter("counter2", "a", "2", "c", "2").increment(42);

    registry.timer("timer1", "a", "1").record(42, TimeUnit.SECONDS);

    registry.distributionSummary("distSummary1", "a", "1").record(47);

    registry.gauge("gauge1", 100.0);
    PolledMeter.update(registry);
  }

  @SuppressWarnings("unchecked")
  private <T> List<T> get(String q) {
    List<T> d1 = (List<T>) endpoint.get(q);

    // Verify that toString on parsed query yields the same result
    SpectatorEndpoint.Query query = SpectatorEndpoint.Query.parse(q);
    List<T> d2 = (List<T>) endpoint.get(query.toString());

    Assert.assertEquals(d1, d2);
    return d1;
  }

  @Test @SuppressWarnings("unchecked")
  public void get() {
    List<Object> datapoints = (List<Object>) endpoint.get();
    Assert.assertEquals(registry.stream().count(), datapoints.size());
  }

  @Test
  public void getTrue() {
    Assert.assertEquals(registry.stream().count(), get(":true").size());
  }

  @Test
  public void getFalse() {
    Assert.assertEquals(0, get(":false").size());
  }

  @Test
  public void getHasName() {
    List<Object> datapoints = get("name,:has");
    Assert.assertEquals(registry.stream().count(), datapoints.size());
  }

  @Test
  public void getHasB() {
    List<Object> datapoints = get("b,:has");
    Assert.assertEquals(1, datapoints.size());
  }

  @Test
  public void getEqual() {
    List<Object> datapoints = get("name,counter1,:eq");
    Assert.assertEquals(1, datapoints.size());
  }

  @Test
  public void getEqualA() {
    List<Object> datapoints = get("a,1,:eq");
    Assert.assertEquals(3, datapoints.size());
  }

  @Test
  public void getLessThan() {
    List<Object> datapoints = get("name,distSummary1,:lt");
    Assert.assertEquals(2, datapoints.size());
  }

  @Test
  public void getLessThanEqual() {
    List<Object> datapoints = get("name,distSummary1,:le");
    Assert.assertEquals(3, datapoints.size());
  }

  @Test
  public void getGreaterThan() {
    List<Object> datapoints = get("name,gauge1,:gt");
    Assert.assertEquals(1, datapoints.size());
  }

  @Test
  public void getGreaterThanEqual() {
    List<Object> datapoints = get("name,gauge1,:ge");
    Assert.assertEquals(2, datapoints.size());
  }

  @Test
  public void getIn1() {
    List<Object> datapoints = get("name,(,gauge1,),:in");
    Assert.assertEquals(1, datapoints.size());
  }

  @Test
  public void getIn2() {
    List<Object> datapoints = get("name,(,counter1,gauge1,),:in");
    Assert.assertEquals(2, datapoints.size());
  }

  @Test
  public void getInEmpty() {
    List<Object> datapoints = get("name,(,),:in");
    Assert.assertEquals(0, datapoints.size());
  }

  @Test
  public void getInNoMatchingKey() {
    List<Object> datapoints = get("z,(,foo,),:in");
    Assert.assertEquals(0, datapoints.size());
  }

  @Test
  public void getCounters() {
    List<SpectatorEndpoint.CounterInfo> datapoints = get("name,counter,:re");
    Assert.assertEquals(2, datapoints.size());
    datapoints.forEach(c -> Assert.assertEquals("counter", c.getType()));
  }

  @Test
  public void getCounter1() {
    List<SpectatorEndpoint.CounterInfo> datapoints = get("name,counter,:re,a,1,:eq,:and");
    Assert.assertEquals(1, datapoints.size());
    Assert.assertEquals(3, datapoints.get(0).getTags().size());
    Assert.assertEquals(1, datapoints.get(0).getCount());
  }

  @Test
  public void getCounter2() {
    List<SpectatorEndpoint.CounterInfo> datapoints = get("name,counter,:re,a,2,:eq,:and");
    Assert.assertEquals(1, datapoints.size());
    Assert.assertEquals(3, datapoints.get(0).getTags().size());
    Assert.assertEquals(42, datapoints.get(0).getCount());
  }

  @Test
  public void getCounter1Or2() {
    List<SpectatorEndpoint.CounterInfo> datapoints = get("name,counter,:re,a,1,:eq,a,2,:eq,:or,:and");
    Assert.assertEquals(2, datapoints.size());
    Assert.assertEquals(3, datapoints.get(0).getTags().size());
    Set<Long> actual = datapoints.stream()
        .map(SpectatorEndpoint.CounterInfo::getCount)
        .collect(Collectors.toSet());
    Set<Long> expected = new HashSet<>();
    expected.add(42L);
    expected.add(1L);
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void getTimers() {
    List<SpectatorEndpoint.TimerInfo> datapoints = get("name,timer,:re");
    Assert.assertEquals(1, datapoints.size());
    datapoints.forEach(c -> Assert.assertEquals("timer", c.getType()));
  }

  @Test
  public void getTimer1() {
    List<SpectatorEndpoint.TimerInfo> datapoints = get("name,timer1,:eq");
    Assert.assertEquals(1, datapoints.size());
    Assert.assertEquals(2, datapoints.get(0).getTags().size());
    Assert.assertEquals(1L, datapoints.get(0).getCount());
    Assert.assertEquals(42000000000L, datapoints.get(0).getTotalTime());
  }

  @Test
  public void getDistSummaries() {
    List<SpectatorEndpoint.DistInfo> datapoints = get("name,distSummary,:re");
    Assert.assertEquals(1, datapoints.size());
    datapoints.forEach(c -> Assert.assertEquals("distribution-summary", c.getType()));
  }

  @Test
  public void getDistSummariesEmptyRE() {
    List<SpectatorEndpoint.DistInfo> datapoints = get("name,distsummary,:re");
    Assert.assertEquals(0, datapoints.size());
  }

  @Test
  public void getDistSummariesREIC() {
    List<SpectatorEndpoint.DistInfo> datapoints = get("name,distsummary,:reic");
    Assert.assertEquals(1, datapoints.size());
    datapoints.forEach(c -> Assert.assertEquals("distribution-summary", c.getType()));
  }

  @Test
  public void getDist1() {
    List<SpectatorEndpoint.DistInfo> datapoints = get("name,distSummary1,:eq");
    Assert.assertEquals(1, datapoints.size());
    Assert.assertEquals(2, datapoints.get(0).getTags().size());
    Assert.assertEquals(1L, datapoints.get(0).getCount());
    Assert.assertEquals(47L, datapoints.get(0).getTotalAmount());
  }

  @Test
  public void getGauge1() {
    List<SpectatorEndpoint.GaugeInfo> datapoints = get("name,gauge1,:eq");
    Assert.assertEquals(1, datapoints.size());
    Assert.assertEquals(1, datapoints.get(0).getTags().size());
    Assert.assertEquals(100.0, datapoints.get(0).getValue(), 1e-12);
  }

}
