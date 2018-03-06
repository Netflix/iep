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
package com.netflix.iep.aws2;

import io.reactivex.Flowable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.DescribeLoadBalancersRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeLoadBalancersResponse;
import software.amazon.awssdk.services.cloudwatch.model.ListMetricsRequest;
import software.amazon.awssdk.services.cloudwatch.model.ListMetricsResponse;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataResponse;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetGroupsResponse;
import software.amazon.awssdk.services.emr.model.ListClustersRequest;
import software.amazon.awssdk.services.emr.model.ListClustersResponse;
import software.amazon.awssdk.services.route53.model.ListHostedZonesRequest;
import software.amazon.awssdk.services.route53.model.ListHostedZonesResponse;
import software.amazon.awssdk.services.route53.model.ListResourceRecordSetsRequest;
import software.amazon.awssdk.services.route53.model.ListResourceRecordSetsResponse;

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
    Function<DescribeAutoScalingGroupsRequest, DescribeAutoScalingGroupsResponse> f = r -> {
      if (r.nextToken() != null) {
        Assert.assertEquals(reqIt.next(), r.nextToken());
      }
      return DescribeAutoScalingGroupsResponse.builder()
          .nextToken(resIt.hasNext() ? resIt.next() : null)
          .build();
    };

    Publisher<DescribeAutoScalingGroupsResponse> publisher =
        Pagination.createPublisher(DescribeAutoScalingGroupsRequest.builder().build(), f);
    Iterable<String> iter = Flowable.fromPublisher(publisher)
        .filter(r -> r.nextToken() != null)
        .map(DescribeAutoScalingGroupsResponse::nextToken)
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
    Function<ListMetricsRequest, ListMetricsResponse> f = r -> {
      if (r.nextToken() != null) {
        Assert.assertEquals(reqIt.next(), r.nextToken());
      }
      return ListMetricsResponse.builder()
          .nextToken(resIt.hasNext() ? resIt.next() : null)
          .build();
    };

    Publisher<ListMetricsResponse> publisher =
        Pagination.createPublisher(ListMetricsRequest.builder().build(), f);
    Iterable<String> iter = Flowable.fromPublisher(publisher)
        .filter(r -> r.nextToken() != null)
        .map(ListMetricsResponse::nextToken)
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
    nextPage.put("abc", AttributeValue.builder().build());
    Map<String, AttributeValue> donePage = new HashMap<>();

    Function<ScanRequest, ScanResponse> f = r -> {
      if (r.exclusiveStartKey() != null) {
        Assert.assertTrue(r.exclusiveStartKey().containsKey("abc"));
      }
      return ScanResponse.builder()
          .lastEvaluatedKey((r.exclusiveStartKey() == null) ? nextPage : donePage)
          .build();
    };

    Publisher<ScanResponse> publisher = Pagination.createPublisher(ScanRequest.builder().build(), f);
    Iterable<ScanResponse> iter = Flowable.fromPublisher(publisher)
        .blockingIterable();

    int count = 0;
    for (ScanResponse r : iter) {
      ++count;
    }

    Assert.assertEquals(2, count);
  }

  @Test
  public void cloudwatchPut() throws Exception {
    final AtomicInteger n = new AtomicInteger();
    Function<PutMetricDataRequest, PutMetricDataResponse> f = r -> {
      if (n.getAndIncrement() > 0) {
        Assert.fail("non-paginated API called more than once");
      }
      return PutMetricDataResponse.builder().build();
    };

    Publisher<PutMetricDataResponse> publisher =
        Pagination.createPublisher(PutMetricDataRequest.builder().build(), f);
    Iterable<PutMetricDataResponse> iter = Flowable.fromPublisher(publisher).blockingIterable();

    int count = 0;
    for (PutMetricDataResponse r : iter) {
      ++count;
    }

    Assert.assertEquals(1, count);
  }

  @Test
  public void ec2() throws Exception {
    SortedSet<String> pages = newPageSet(5);
    final Iterator<String> reqIt = pages.iterator();
    final Iterator<String> resIt = pages.iterator();
    Function<DescribeInstancesRequest, DescribeInstancesResponse> f = r -> {
      if (r.nextToken() != null) {
        Assert.assertEquals(reqIt.next(), r.nextToken());
      }
      return DescribeInstancesResponse.builder()
          .nextToken(resIt.hasNext() ? resIt.next() : null)
          .build();
    };

    Publisher<DescribeInstancesResponse> publisher =
        Pagination.createPublisher(DescribeInstancesRequest.builder().build(), f);
    Iterable<String> iter = Flowable.fromPublisher(publisher)
        .filter(r -> r.nextToken() != null)
        .map(DescribeInstancesResponse::nextToken)
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
    Function<DescribeLoadBalancersRequest, DescribeLoadBalancersResponse> f = r -> {
      if (r.nextToken() != null) {
        Assert.assertEquals(reqIt.next(), r.nextToken());
      }
      return DescribeLoadBalancersResponse.builder()
          .nextToken(resIt.hasNext() ? resIt.next() : null)
          .build();
    };

    Publisher<DescribeLoadBalancersResponse> publisher =
        Pagination.createPublisher(DescribeLoadBalancersRequest.builder().build(), f);
    Iterable<String> iter = Flowable.fromPublisher(publisher)
        .filter(r -> r.nextToken() != null)
        .map(DescribeLoadBalancersResponse::nextToken)
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
    Function<DescribeTargetGroupsRequest, DescribeTargetGroupsResponse> f = r -> {
      if (r.marker() != null) {
        Assert.assertEquals(reqIt.next(), r.marker());
      }
      return DescribeTargetGroupsResponse.builder()
          .nextMarker(resIt.hasNext() ? resIt.next() : null)
          .build();
    };

    Publisher<DescribeTargetGroupsResponse> publisher =
        Pagination.createPublisher(DescribeTargetGroupsRequest.builder().build(), f);
    Iterable<String> iter = Flowable.fromPublisher(publisher)
        .filter(r -> r.nextMarker() != null)
        .map(DescribeTargetGroupsResponse::nextMarker)
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
    Function<ListClustersRequest, ListClustersResponse> f = r -> {
      if (r.marker() != null) {
        Assert.assertEquals(reqIt.next(), r.marker());
      }
      return ListClustersResponse.builder()
          .marker(resIt.hasNext() ? resIt.next() : null)
          .build();
    };

    Publisher<ListClustersResponse> publisher =
        Pagination.createPublisher(ListClustersRequest.builder().build(), f);
    Iterable<String> iter = Flowable.fromPublisher(publisher)
        .filter(r -> r.marker() != null)
        .map(ListClustersResponse::marker)
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
    Function<ListHostedZonesRequest, ListHostedZonesResponse> f = r -> {
      if (r.marker() != null) {
        Assert.assertEquals(reqIt.next(), r.marker());
      }
      return ListHostedZonesResponse.builder()
          .nextMarker(resIt.hasNext() ? resIt.next() : null)
          .build();
    };

    Publisher<ListHostedZonesResponse> publisher =
        Pagination.createPublisher(ListHostedZonesRequest.builder().build(), f);
    Iterable<String> iter = Flowable.fromPublisher(publisher)
        .filter(r -> r.nextMarker() != null)
        .map(ListHostedZonesResponse::nextMarker)
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
    Function<ListResourceRecordSetsRequest, ListResourceRecordSetsResponse> f = r -> {
      if (r.startRecordName() != null) {
        String expected = reqIt.next();
        Assert.assertEquals(expected + "-id", r.startRecordIdentifier());
        Assert.assertEquals(expected + "-name", r.startRecordName());
        // TODO: why is this null?
        //Assert.assertEquals(expected + "-type", r.startRecordType());
      }

      String next = resIt.hasNext() ? resIt.next() : null;
      return ListResourceRecordSetsResponse.builder()
          .nextRecordIdentifier((next != null) ? next + "-id" : null)
          .nextRecordName((next != null) ? next + "-name" : null)
          .nextRecordType((next != null) ? next + "-type" : null)
          .build();
    };

    Publisher<ListResourceRecordSetsResponse> publisher =
        Pagination.createPublisher(ListResourceRecordSetsRequest.builder().build(), f);
    Iterable<String> iter = Flowable.fromPublisher(publisher)
        .filter(r -> r.nextRecordName() != null)
        .map(ListResourceRecordSetsResponse::nextRecordName)
        .blockingIterable();

    SortedSet<String> results = new TreeSet<>();
    for (String s : iter) {
      results.add(s.substring(0, 5));
    }

    Assert.assertEquals(pages, results);
    Assert.assertFalse(reqIt.hasNext());
  }

}
