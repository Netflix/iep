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
package com.netflix.iep.aws;

import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.ListMetricsResult;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.PutMetricDataResult;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsResult;
import com.amazonaws.services.elasticmapreduce.model.ListClustersRequest;
import com.amazonaws.services.elasticmapreduce.model.ListClustersResult;
import com.amazonaws.services.route53.model.ListHostedZonesRequest;
import com.amazonaws.services.route53.model.ListHostedZonesResult;
import com.amazonaws.services.route53.model.ListResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ListResourceRecordSetsResult;
import io.reactivex.Flowable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.reactivestreams.Publisher;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@RunWith(JUnit4.class)
public class PaginationTest {

  private SortedSet<String> newPageSet(int n) {
    SortedSet<String> pages = new TreeSet<>();
    for (int i = 0; i < n; ++i) {
      pages.add(String.format("%05d", i));
    }
    return pages;
  }

  private void autoscalingN(int n) throws Exception {
    SortedSet<String> pages = newPageSet(n);
    final Iterator<String> reqIt = pages.iterator();
    final Iterator<String> resIt = pages.iterator();
    Function<DescribeAutoScalingGroupsRequest, DescribeAutoScalingGroupsResult> f = r -> {
      if (r.getNextToken() != null) {
        Assert.assertEquals(reqIt.next(), r.getNextToken());
      }
      return new DescribeAutoScalingGroupsResult()
          .withNextToken(resIt.hasNext() ? resIt.next() : null);
    };

    Publisher<DescribeAutoScalingGroupsResult> publisher =
        Pagination.createPublisher(new DescribeAutoScalingGroupsRequest(), f);
    Iterable<String> iter = Flowable.fromPublisher(publisher)
        .filter(r -> r.getNextToken() != null)
        .map(DescribeAutoScalingGroupsResult::getNextToken)
        .blockingIterable();

    SortedSet<String> results = new TreeSet<>();
    for (String s : iter) {
      results.add(s);
    }

    Assert.assertEquals(pages, results);
    Assert.assertFalse(reqIt.hasNext());
  }

  @Test
  public void autoscaling() throws Exception {
    autoscalingN(5);
  }

  @Test
  public void autoscalingNoStackOverflow() throws Exception {
    autoscalingN(10000);
  }

  @Test
  public void cloudwatch() throws Exception {
    SortedSet<String> pages = newPageSet(5);
    final Iterator<String> reqIt = pages.iterator();
    final Iterator<String> resIt = pages.iterator();
    Function<ListMetricsRequest, ListMetricsResult> f = r -> {
      if (r.getNextToken() != null) {
        Assert.assertEquals(reqIt.next(), r.getNextToken());
      }
      return new ListMetricsResult()
          .withNextToken(resIt.hasNext() ? resIt.next() : null);
    };

    Publisher<ListMetricsResult> publisher =
        Pagination.createPublisher(new ListMetricsRequest(), f);
    Iterable<String> iter = Flowable.fromPublisher(publisher)
        .filter(r -> r.getNextToken() != null)
        .map(ListMetricsResult::getNextToken)
        .blockingIterable();

    SortedSet<String> results = new TreeSet<>();
    for (String s : iter) {
      results.add(s);
    }

    Assert.assertEquals(pages, results);
    Assert.assertFalse(reqIt.hasNext());
  }

  @Test
  public void dynamoDB() throws Exception {
    Map<String, AttributeValue> nextPage = new HashMap<>();
    nextPage.put("abc", new AttributeValue());
    Map<String, AttributeValue> donePage = new HashMap<>();

    Function<ScanRequest, ScanResult> f = r -> {
      if (r.getExclusiveStartKey() != null) {
        Assert.assertTrue(r.getExclusiveStartKey().containsKey("abc"));
      }
      return new ScanResult()
          .withLastEvaluatedKey((r.getExclusiveStartKey() == null) ? nextPage : donePage);
    };

    Publisher<ScanResult> publisher = Pagination.createPublisher(new ScanRequest(), f);
    Iterable<ScanResult> iter = Flowable.fromPublisher(publisher)
        .blockingIterable();

    int count = 0;
    for (ScanResult r : iter) {
      ++count;
    }

    Assert.assertEquals(2, count);
  }

  @Test
  public void cloudwatchPut() throws Exception {
    final AtomicInteger n = new AtomicInteger();
    Function<PutMetricDataRequest, PutMetricDataResult> f = r -> {
      if (n.getAndIncrement() > 0) {
        Assert.fail("non-paginated API called more than once");
      }
      return new PutMetricDataResult();
    };

    Publisher<PutMetricDataResult> publisher =
        Pagination.createPublisher(new PutMetricDataRequest(), f);
    Iterable<PutMetricDataResult> iter = Flowable.fromPublisher(publisher).blockingIterable();

    int count = 0;
    for (PutMetricDataResult r : iter) {
      ++count;
    }

    Assert.assertEquals(1, count);
  }

  @Test
  public void ec2() throws Exception {
    SortedSet<String> pages = newPageSet(5);
    final Iterator<String> reqIt = pages.iterator();
    final Iterator<String> resIt = pages.iterator();
    Function<DescribeInstancesRequest, DescribeInstancesResult> f = r -> {
      if (r.getNextToken() != null) {
        Assert.assertEquals(reqIt.next(), r.getNextToken());
      }
      return new DescribeInstancesResult()
          .withNextToken(resIt.hasNext() ? resIt.next() : null);
    };

    Publisher<DescribeInstancesResult> publisher =
        Pagination.createPublisher(new DescribeInstancesRequest(), f);
    Iterable<String> iter = Flowable.fromPublisher(publisher)
        .filter(r -> r.getNextToken() != null)
        .map(DescribeInstancesResult::getNextToken)
        .blockingIterable();

    SortedSet<String> results = new TreeSet<>();
    for (String s : iter) {
      results.add(s);
    }

    Assert.assertEquals(pages, results);
    Assert.assertFalse(reqIt.hasNext());
  }

  @Test
  public void elb() throws Exception {
    SortedSet<String> pages = newPageSet(5);
    final Iterator<String> reqIt = pages.iterator();
    final Iterator<String> resIt = pages.iterator();
    Function<DescribeLoadBalancersRequest, DescribeLoadBalancersResult> f = r -> {
      if (r.getMarker() != null) {
        Assert.assertEquals(reqIt.next(), r.getMarker());
      }
      return new DescribeLoadBalancersResult()
          .withNextMarker(resIt.hasNext() ? resIt.next() : null);
    };

    Publisher<DescribeLoadBalancersResult> publisher =
        Pagination.createPublisher(new DescribeLoadBalancersRequest(), f);
    Iterable<String> iter = Flowable.fromPublisher(publisher)
        .filter(r -> r.getNextMarker() != null)
        .map(DescribeLoadBalancersResult::getNextMarker)
        .blockingIterable();

    SortedSet<String> results = new TreeSet<>();
    for (String s : iter) {
      results.add(s);
    }

    Assert.assertEquals(pages, results);
    Assert.assertFalse(reqIt.hasNext());
  }

  @Test
  public void elbv2() throws Exception {
    SortedSet<String> pages = newPageSet(5);
    final Iterator<String> reqIt = pages.iterator();
    final Iterator<String> resIt = pages.iterator();
    Function<DescribeTargetGroupsRequest, DescribeTargetGroupsResult> f = r -> {
      if (r.getMarker() != null) {
        Assert.assertEquals(reqIt.next(), r.getMarker());
      }
      return new DescribeTargetGroupsResult()
          .withNextMarker(resIt.hasNext() ? resIt.next() : null);
    };

    Publisher<DescribeTargetGroupsResult> publisher =
        Pagination.createPublisher(new DescribeTargetGroupsRequest(), f);
    Iterable<String> iter = Flowable.fromPublisher(publisher)
        .filter(r -> r.getNextMarker() != null)
        .map(DescribeTargetGroupsResult::getNextMarker)
        .blockingIterable();

    SortedSet<String> results = new TreeSet<>();
    for (String s : iter) {
      results.add(s);
    }

    Assert.assertEquals(pages, results);
    Assert.assertFalse(reqIt.hasNext());
  }

  @Test
  public void emr() throws Exception {
    SortedSet<String> pages = newPageSet(5);
    final Iterator<String> reqIt = pages.iterator();
    final Iterator<String> resIt = pages.iterator();
    Function<ListClustersRequest, ListClustersResult> f = r -> {
      if (r.getMarker() != null) {
        Assert.assertEquals(reqIt.next(), r.getMarker());
      }
      return new ListClustersResult()
          .withMarker(resIt.hasNext() ? resIt.next() : null);
    };

    Publisher<ListClustersResult> publisher =
        Pagination.createPublisher(new ListClustersRequest(), f);
    Iterable<String> iter = Flowable.fromPublisher(publisher)
        .filter(r -> r.getMarker() != null)
        .map(ListClustersResult::getMarker)
        .blockingIterable();

    SortedSet<String> results = new TreeSet<>();
    for (String s : iter) {
      results.add(s);
    }

    Assert.assertEquals(pages, results);
    Assert.assertFalse(reqIt.hasNext());
  }

  @Test
  public void route53HostedZones() throws Exception {
    SortedSet<String> pages = newPageSet(5);
    final Iterator<String> reqIt = pages.iterator();
    final Iterator<String> resIt = pages.iterator();
    Function<ListHostedZonesRequest, ListHostedZonesResult> f = r -> {
      if (r.getMarker() != null) {
        Assert.assertEquals(reqIt.next(), r.getMarker());
      }
      return new ListHostedZonesResult()
          .withNextMarker(resIt.hasNext() ? resIt.next() : null);
    };

    Publisher<ListHostedZonesResult> publisher =
        Pagination.createPublisher(new ListHostedZonesRequest(), f);
    Iterable<String> iter = Flowable.fromPublisher(publisher)
        .filter(r -> r.getNextMarker() != null)
        .map(ListHostedZonesResult::getNextMarker)
        .blockingIterable();

    SortedSet<String> results = new TreeSet<>();
    for (String s : iter) {
      results.add(s);
    }

    Assert.assertEquals(pages, results);
    Assert.assertFalse(reqIt.hasNext());
  }

  @Test
  public void route53ResourceRecordSets() throws Exception {
    SortedSet<String> pages = newPageSet(5);
    final Iterator<String> reqIt = pages.iterator();
    final Iterator<String> resIt = pages.iterator();
    Function<ListResourceRecordSetsRequest, ListResourceRecordSetsResult> f = r -> {
      if (r.getStartRecordName() != null) {
        String expected = reqIt.next();
        Assert.assertEquals(expected + "-id", r.getStartRecordIdentifier());
        Assert.assertEquals(expected + "-name", r.getStartRecordName());
        Assert.assertEquals(expected + "-type", r.getStartRecordType());
      }

      String next = resIt.hasNext() ? resIt.next() : null;
      return new ListResourceRecordSetsResult()
          .withNextRecordIdentifier((next != null) ? next + "-id" : null)
          .withNextRecordName((next != null) ? next + "-name" : null)
          .withNextRecordType((next != null) ? next + "-type" : null);
    };

    Publisher<ListResourceRecordSetsResult> publisher =
        Pagination.createPublisher(new ListResourceRecordSetsRequest(), f);
    Iterable<String> iter = Flowable.fromPublisher(publisher)
        .filter(r -> r.getNextRecordName() != null)
        .map(ListResourceRecordSetsResult::getNextRecordName)
        .blockingIterable();

    SortedSet<String> results = new TreeSet<>();
    for (String s : iter) {
      results.add(s.substring(0, 5));
    }

    Assert.assertEquals(pages, results);
    Assert.assertFalse(reqIt.hasNext());
  }

}
