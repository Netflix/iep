/*
 * Copyright 2014-2017 Netflix, Inc.
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

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import org.reactivestreams.Publisher;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Helper for paginating AWS requests.
 */
public final class Pagination {


  private static final Executor DEFAULT_POOL = Executors.newCachedThreadPool(new ThreadFactory() {
    private final AtomicInteger nextId = new AtomicInteger();
    @Override public Thread newThread(Runnable r) {
      return new Thread(r, "aws-publisher-" + nextId.getAndIncrement());
    }
  });

  private static final String[] GET_NEXT = {
    "getNextToken", "getNextMarker", "getMarker"
  };

  private static final String[] SET_NEXT = {
    "setNextToken", "setMarker", "setMarker"
  };

  /**
   * Create an iterator over all results for a given request type. Example usage:
   *
   * <pre>
   * AmazonEC2 ec2Client = ...
   * Iterator&lt;DescribeInstancesResult&gt; it =
   *   Pagination.createIterator(new DescribeInstancesRequest(), client::describeInstances);
   * while (it.hasNext()) {
   *   DescribeInstancesResult result = it.next();
   *   ...
   * }
   * </pre>
   *
   * @param request
   *     Initial request to send. This request will get modified with the new page token
   *     for subsequent requests so it should not be shared across calls.
   * @param f
   *     Function for mapping the request to a result.
   * @return
   *     Iterator over all results for a given request.
   */
  public static <R, T> Iterator<T> createIterator(R request, Function<R, T> f) {
    return new Iterator<T>() {
      private R nextReq = request;
      private T res = null;

      @Override public boolean hasNext() {
        return nextReq != null;
      }

      @Override public T next() {
        res = f.apply(nextReq);
        nextReq = getNextRequest(nextReq, res);
        return res;
      }
    };
  }

  /**
   * Create an iterable for obtaining an iterator over all results for a given request type.
   * Example usage:
   *
   * <pre>
   * AmazonEC2 ec2Client = ...
   * DescribeInstancesRequest req = new DescribeInstancesRequest();
   * for (DescribeInstancesResult res : Pagination.createIterable(req, client::desribeInstances)) {
   *   ...
   * }
   * </pre>
   *
   * @param request
   *     Initial request to send. This request will get modified with the new page token
   *     for subsequent requests so it should not be shared across calls.
   * @param f
   *     Function for mapping the request to a result.
   * @return
   *     Iterable for obtaining an iterator over all results for a given request.
   */
  public static <R, T> Iterable<T> createIterable(R request, Function<R, T> f) {
    return () -> createIterator(request, f);
  }

  /**
   * Create a reactive streams publisher for obtaining all results for a given request type.
   * The requests will be made using a cached thread pool that will spin up new threads as
   * needed. Example usage with rxjava2:
   *
   * <pre>
   * AmazonEC2 ec2Client = ...
   * DescribeInstancesRequest req = new DescribeInstancesRequest();
   * Iterable&lt;String&gt; instanceIds = Flowable
   *   .fromPublisher(Pagination.createPublisher(req, client::describeInstances))
   *   .flatMap(r -> Flowable.fromIterable(r.getReservations()))
   *   .flatMap(r -> Flowable.fromIterable(r.getInstances()))
   *   .map(Instance::getInstanceId)
   *   .blockingIterable();
   * for (String instanceId : instanceIds) {
   *   ...
   * }
   * </pre>
   *
   * @param request
   *     Initial request to send. This request will get modified with the new page token
   *     for subsequent requests so it should not be shared across calls.
   * @param f
   *     Function for mapping the request to a result.
   * @return
   *     Publisher for obtaining all results for a given request.
   */
  public static <R, T> Publisher<T> createPublisher(R request, Function<R, T> f) {
    return createPublisher(DEFAULT_POOL, request, f);
  }

  /**
   * Create a reactive streams publisher for obtaining all results for a given request type.
   * Example usage with rxjava2:
   *
   * <pre>
   * Executor myPool = Executors.newFixedThreadPool(10);
   * AmazonEC2 ec2Client = ...
   * DescribeInstancesRequest req = new DescribeInstancesRequest();
   * Iterable&lt;String&gt; instanceIds = Flowable
   *   .fromPublisher(Pagination.createPublisher(myPool, req, client::describeInstances))
   *   .flatMap(r -> Flowable.fromIterable(r.getReservations()))
   *   .flatMap(r -> Flowable.fromIterable(r.getInstances()))
   *   .map(Instance::getInstanceId)
   *   .blockingIterable();
   * for (String instanceId : instanceIds) {
   *   ...
   * }
   * </pre>
   *
   * @param executor
   *     Executor that will be used for making the AWS requests.
   * @param request
   *     Initial request to send. This request will get modified with the new page token
   *     for subsequent requests so it should not be shared across calls.
   * @param f
   *     Function for mapping the request to a result.
   * @return
   *     Publisher for obtaining all results for a given request.
   */
  public static <R, T> Publisher<T> createPublisher(Executor executor, R request, Function<R, T> f) {
    return Flowable.just(request)
        .observeOn(Schedulers.from(executor))
        .flatMap(r -> Flowable.fromIterable(createIterable(r, f)))
        .observeOn(Schedulers.computation());
  }

  private static <R, T> R getNextRequest(R request, T result) {
    try {
      for (int i = 0; i < GET_NEXT.length; ++i) {
        for (Method getter : result.getClass().getMethods()) {
          if (getter.getName().equals(GET_NEXT[i])) {
            Object next = getter.invoke(result);
            if (next == null) {
              return null;
            } else {
              Method setter = request.getClass().getMethod(SET_NEXT[i], String.class);
              setter.invoke(request, next);
              return request;
            }
          }
        }
      }
      return null;
    } catch (Exception e) {
      throw new IllegalStateException("failed to set next token", e);
    }
  }

}
