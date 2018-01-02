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

import com.netflix.iep.admin.HttpEndpoint;
import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.DistributionSummary;
import com.netflix.spectator.api.Gauge;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Measurement;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Tag;
import com.netflix.spectator.api.Timer;
import com.netflix.spectator.impl.Preconditions;

import javax.inject.Inject;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * List measurements via Spectator. The path can be an Atlas query expression
 * to further restrict the set of return values. For more information on expressions
 * see:
 *
 * https://github.com/Netflix/atlas/wiki/Reference-query
 */
public class SpectatorEndpoint implements HttpEndpoint {

  private final Registry registry;

  @Inject
  public SpectatorEndpoint(Registry registry) {
    this.registry = registry;
  }

  @Override public Object get() {
    return get(":true");
  }

  @Override public Object get(String path) {
    Query q = Query.parse(path);
    return registry.stream()
        .filter(m -> !m.hasExpired())
        .flatMap(m -> {
          List<Object> ms = new ArrayList<>();
          Map<String, String> tags = toMap(m.id());
          if (m instanceof Counter) {
            add(q, tags, ms, new CounterInfo(tags, ((Counter) m).count()));
          } else if (m instanceof Timer) {
            Timer t = (Timer) m;
            add(q, tags, ms, new TimerInfo(tags, t.totalTime(), t.count()));
          } else if (m instanceof DistributionSummary) {
            DistributionSummary t = (DistributionSummary) m;
            add(q, tags, ms, new DistInfo(tags, t.totalAmount(), t.count()));
          } else if (m instanceof Gauge) {
            Gauge g = (Gauge) m;
            add(q, tags, ms, new GaugeInfo(tags, g.value()));
          }
          return ms.stream();
        })
        .collect(Collectors.toList());
  }

  private void add(Query q, Map<String, String> tags, List<Object> vs, Object obj) {
    if (q.matches(tags)) {
      vs.add(obj);
    }
  }

  private Map<String, String> toMap(Id id) {
    Map<String, String> tags = new HashMap<>();
    tags.put("name", id.name());
    for (Tag t : id.tags()) {
      tags.put(t.key(), t.value());
    }
    return tags;
  }

  public static class CounterInfo {

    private final Map<String, String> tags;
    private final long count;

    public CounterInfo(Map<String, String> tags, long count) {
      this.tags = tags;
      this.count = count;
    }

    public String getType() {
      return "counter";
    }

    public Map<String, String> getTags() {
      return tags;
    }

    public long getCount() {
      return count;
    }

    @Override public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      CounterInfo that = (CounterInfo) o;
      return count == that.count && tags.equals(that.tags);
    }

    @Override public int hashCode() {
      int result = tags.hashCode();
      result = 31 * result + (int) (count ^ (count >>> 32));
      return result;
    }

    @Override public String toString() {
      return "CounterInfo(" + tags + ", " + count + ")";
    }
  }

  public static class TimerInfo {

    private final Map<String, String> tags;
    private final long totalTime;
    private final long count;

    public TimerInfo(Map<String, String> tags, long totalTime, long count) {
      this.tags = tags;
      this.totalTime = totalTime;
      this.count = count;
    }

    public String getType() {
      return "timer";
    }

    public Map<String, String> getTags() {
      return tags;
    }

    public long getTotalTime() {
      return totalTime;
    }

    public long getCount() {
      return count;
    }

    @Override public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      TimerInfo timerInfo = (TimerInfo) o;
      return totalTime == timerInfo.totalTime
          && count == timerInfo.count
          && tags.equals(timerInfo.tags);
    }

    @Override public int hashCode() {
      int result = tags.hashCode();
      result = 31 * result + (int) (totalTime ^ (totalTime >>> 32));
      result = 31 * result + (int) (count ^ (count >>> 32));
      return result;
    }

    @Override public String toString() {
      return "TimerInfo(" + tags + ", " + count + ", " + totalTime + ")";
    }
  }

  public static class DistInfo {

    private final Map<String, String> tags;
    private final long totalAmount;
    private final long count;

    public DistInfo(Map<String, String> tags, long totalAmount, long count) {
      this.tags = tags;
      this.totalAmount = totalAmount;
      this.count = count;
    }

    public String getType() {
      return "distribution-summary";
    }

    public Map<String, String> getTags() {
      return tags;
    }

    public long getTotalAmount() {
      return totalAmount;
    }

    public long getCount() {
      return count;
    }

    @Override public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      DistInfo distInfo = (DistInfo) o;
      return totalAmount == distInfo.totalAmount
          && count == distInfo.count
          && tags.equals(distInfo.tags);
    }

    @Override public int hashCode() {
      int result = tags.hashCode();
      result = 31 * result + (int) (totalAmount ^ (totalAmount >>> 32));
      result = 31 * result + (int) (count ^ (count >>> 32));
      return result;
    }

    @Override public String toString() {
      return "DistInfo(" + tags + ", " + count + ", " + totalAmount + ")";
    }
  }

  public static class GaugeInfo {

    private final Map<String, String> tags;
    private final double value;

    public GaugeInfo(Map<String, String> tags, double value) {
      this.tags = tags;
      this.value = value;
    }

    public String getType() {
      return "gauge";
    }

    public Map<String, String> getTags() {
      return tags;
    }

    public double getValue() {
      return value;
    }

    @Override public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      GaugeInfo gaugeInfo = (GaugeInfo) o;
      return Double.compare(gaugeInfo.value, value) == 0 && tags.equals(gaugeInfo.tags);
    }

    @Override public int hashCode() {
      int result;
      long temp;
      result = tags.hashCode();
      temp = Double.doubleToLongBits(value);
      result = 31 * result + (int) (temp ^ (temp >>> 32));
      return result;
    }

    @Override public String toString() {
      return "GaugeInfo(" + tags + ", " + value + ")";
    }
  }

  interface Query {

    Query TRUE = new Query() {
      @Override public boolean matches(Map<String, String> tags) {
        return true;
      }

      @Override public String toString() {
        return ":true";
      }
    };

    Query FALSE = new Query() {
      @Override public boolean matches(Map<String, String> tags) {
        return false;
      }

      @Override public String toString() {
        return ":false";
      }
    };

    @SuppressWarnings("unchecked")
    static Query parse(String expr) {
      Query q, q1, q2;
      String k, v;
      List<String> vs = null;
      String[] parts = expr.split(",");
      Deque<Object> stack = new ArrayDeque<>(parts.length);
      for (String p : parts) {
        String token = p.trim();
        if (vs != null && !")".equals(token)) {
          vs.add(token);
          continue;
        }
        switch (token) {
          case "(":
            vs = new ArrayList<>();
            break;
          case ")":
            stack.push(vs);
            vs = null;
            break;
          case ":true":
            stack.push(TRUE);
            break;
          case ":false":
            stack.push(FALSE);
            break;
          case ":and":
            q2 = (Query) stack.pop();
            q1 = (Query) stack.pop();
            stack.push(new AndQuery(q1, q2));
            break;
          case ":or":
            q2 = (Query) stack.pop();
            q1 = (Query) stack.pop();
            stack.push(new OrQuery(q1, q2));
            break;
          case ":not":
            q = (Query) stack.pop();
            stack.push(new NotQuery(q));
            break;
          case ":has":
            k = (String) stack.pop();
            stack.push(new HasQuery(k));
            break;
          case ":eq":
            v = (String) stack.pop();
            k = (String) stack.pop();
            stack.push(new EqualQuery(k, v));
            break;
          case ":in":
            vs = (List<String>) stack.pop();
            k = (String) stack.pop();
            stack.push(new InQuery(k, new TreeSet<>(vs)));
            break;
          case ":lt":
            v = (String) stack.pop();
            k = (String) stack.pop();
            stack.push(new LessThanQuery(k, v));
            break;
          case ":le":
            v = (String) stack.pop();
            k = (String) stack.pop();
            stack.push(new LessThanEqualQuery(k, v));
            break;
          case ":gt":
            v = (String) stack.pop();
            k = (String) stack.pop();
            stack.push(new GreaterThanQuery(k, v));
            break;
          case ":ge":
            v = (String) stack.pop();
            k = (String) stack.pop();
            stack.push(new GreaterThanEqualQuery(k, v));
            break;
          case ":re":
            v = (String) stack.pop();
            k = (String) stack.pop();
            stack.push(new RegexQuery(k, v));
            break;
          case ":reic":
            v = (String) stack.pop();
            k = (String) stack.pop();
            stack.push(new RegexQuery(k, v, Pattern.CASE_INSENSITIVE, ":reic"));
            break;
          default:
            if (token.startsWith(":")) {
              throw new IllegalArgumentException("unknown word '" + token + "'");
            }
            stack.push(token);
            break;
        }
      }
      q = (Query) stack.pop();
      if (!stack.isEmpty()) {
        throw new IllegalArgumentException("too many items remaining on stack: " + stack);
      }
      return q;
    }

    boolean matches(Map<String, String> tags);

    default Query and(Query q) {
      return new AndQuery(this, q);
    }

    default Query or(Query q) {
      return new OrQuery(this, q);
    }

    default Query not() {
      return new NotQuery(this);
    }
  }

  private static class AndQuery implements Query {
    private final Query q1;
    private final Query q2;

    AndQuery(Query q1, Query q2) {
      this.q1 = Preconditions.checkNotNull(q1, "q1");
      this.q2 = Preconditions.checkNotNull(q2, "q2");
    }

    @Override public boolean matches(Map<String, String> tags) {
      return q1.matches(tags) && q2.matches(tags);
    }

    @Override public String toString() {
      return q1 + "," + q2 + ",:and";
    }
  }

  private static class OrQuery implements Query {
    private final Query q1;
    private final Query q2;

    OrQuery(Query q1, Query q2) {
      this.q1 = Preconditions.checkNotNull(q1, "q1");
      this.q2 = Preconditions.checkNotNull(q2, "q2");
    }

    @Override public boolean matches(Map<String, String> tags) {
      return q1.matches(tags) || q2.matches(tags);
    }

    @Override public String toString() {
      return q1 + "," + q2 + ",:or";
    }
  }

  private static class NotQuery implements Query {
    private final Query q;

    NotQuery(Query q) {
      this.q = Preconditions.checkNotNull(q, "q");
    }

    @Override public boolean matches(Map<String, String> tags) {
      return !q.matches(tags);
    }

    @Override public String toString() {
      return q + ",:not";
    }
  }

  private static class HasQuery implements Query {
    private final String k;

    HasQuery(String k) {
      this.k = Preconditions.checkNotNull(k, "k");
    }

    @Override public boolean matches(Map<String, String> tags) {
      return tags.containsKey(k);
    }

    @Override public String toString() {
      return k + ",:has";
    }
  }

  private static class EqualQuery implements Query {
    private final String k;
    private final String v;

    EqualQuery(String k, String v) {
      this.k = Preconditions.checkNotNull(k, "k");
      this.v = Preconditions.checkNotNull(v, "v");
    }

    @Override public boolean matches(Map<String, String> tags) {
      return v.equals(tags.get(k));
    }

    @Override public String toString() {
      return k + "," + v + ",:eq";
    }
  }

  private static class InQuery implements Query {
    private final String k;
    private final Set<String> vs;

    InQuery(String k, Set<String> v) {
      this.k = Preconditions.checkNotNull(k, "k");
      this.vs = Preconditions.checkNotNull(v, "vs");
    }

    @Override public boolean matches(Map<String, String> tags) {
      String s = tags.get(k);
      return s != null && vs.contains(tags.get(k));
    }

    @Override public String toString() {
      String values = vs.stream().collect(Collectors.joining(","));
      return k + ",(," + values + ",),:in";
    }
  }

  private static class LessThanQuery implements Query {
    private final String k;
    private final String v;

    LessThanQuery(String k, String v) {
      this.k = Preconditions.checkNotNull(k, "k");
      this.v = Preconditions.checkNotNull(v, "v");
    }

    @Override public boolean matches(Map<String, String> tags) {
      String s = tags.get(k);
      return s != null && s.compareTo(v) < 0;
    }

    @Override public String toString() {
      return k + "," + v + ",:lt";
    }
  }

  private static class LessThanEqualQuery implements Query {
    private final String k;
    private final String v;

    LessThanEqualQuery(String k, String v) {
      this.k = Preconditions.checkNotNull(k, "k");
      this.v = Preconditions.checkNotNull(v, "v");
    }

    @Override public boolean matches(Map<String, String> tags) {
      String s = tags.get(k);
      return s != null && s.compareTo(v) <= 0;
    }

    @Override public String toString() {
      return k + "," + v + ",:le";
    }
  }

  private static class GreaterThanQuery implements Query {
    private final String k;
    private final String v;

    GreaterThanQuery(String k, String v) {
      this.k = Preconditions.checkNotNull(k, "k");
      this.v = Preconditions.checkNotNull(v, "v");
    }

    @Override public boolean matches(Map<String, String> tags) {
      String s = tags.get(k);
      return s != null && s.compareTo(v) > 0;
    }

    @Override public String toString() {
      return k + "," + v + ",:gt";
    }
  }

  private static class GreaterThanEqualQuery implements Query {
    private final String k;
    private final String v;

    GreaterThanEqualQuery(String k, String v) {
      this.k = Preconditions.checkNotNull(k, "k");
      this.v = Preconditions.checkNotNull(v, "v");
    }

    @Override public boolean matches(Map<String, String> tags) {
      String s = tags.get(k);
      return s != null && s.compareTo(v) >= 0;
    }

    @Override public String toString() {
      return k + "," + v + ",:ge";
    }
  }

  private static class RegexQuery implements Query {
    private final String k;
    private final String v;
    private final Pattern pattern;
    private final String name;

    RegexQuery(String k, String v) {
      this(k, v, 0, ":re");
    }

    RegexQuery(String k, String v, int flags, String name) {
      this.k = Preconditions.checkNotNull(k, "k");
      this.v = Preconditions.checkNotNull(v, "v");
      this.pattern = Pattern.compile("^" + v, flags);
      this.name = Preconditions.checkNotNull(name, "name");
    }

    @Override public boolean matches(Map<String, String> tags) {
      String s = tags.get(k);
      return s != null && pattern.matcher(s).find();
    }

    @Override public String toString() {
      return k + "," + v + "," + name;
    }
  }
}
