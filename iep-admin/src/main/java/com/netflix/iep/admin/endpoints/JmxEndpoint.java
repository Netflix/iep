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

import com.netflix.iep.admin.ErrorMessage;
import com.netflix.iep.admin.HttpEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides a read-only view of JMX.
 */
public class JmxEndpoint implements HttpEndpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(JmxEndpoint.class);

  private final MBeanServer mBeanServer;

  /**
   * Create a new instance using the local platform MBean server.
   */
  @Inject
  public JmxEndpoint() {
    this(ManagementFactory.getPlatformMBeanServer());
  }

  /**
   * Create a new instance.
   *
   * @param mBeanServer
   *     Server to query. This is typically {@link ManagementFactory#getPlatformMBeanServer()}.
   */
  public JmxEndpoint(MBeanServer mBeanServer) {
    this.mBeanServer = mBeanServer;
  }

  @Override public Object get() {
    return get("*:*");
  }

  @Override public Object get(String path) {
    try {
      ObjectName query = new ObjectName(path);
      return mBeanServer.queryNames(query, null)
          .stream()
          .flatMap(this::get)
          .collect(Collectors.toList());
    } catch (MalformedObjectNameException e) {
      return new ErrorMessage(400, e);
    } catch (Exception e) {
      LOGGER.debug("failed to query MBean server: " + path, e);
      return new ErrorMessage(e);
    }
  }

  private Stream<JmxBean> get(ObjectName name) {
    try {
      MBeanInfo info = mBeanServer.getMBeanInfo(name);
      String[] attrNames = mapToNames(info.getAttributes());
      AttributeList attrs = mBeanServer.getAttributes(name, attrNames);
      Map<String, Object> attributes = new HashMap<>();
      for (Attribute attr : attrs.asList()) {
        attributes.put(attr.getName(), fixValue(attr.getValue()));
      }
      return Stream.of(new JmxBean(JmxId.create(name), attributes));
    } catch (Exception e) {
      LOGGER.debug("failed to get MBeanInfo for " + name, e);
      return Stream.empty();
    }
  }

  private String[] mapToNames(MBeanAttributeInfo[] attrs) {
    String[] names = new String[attrs.length];
    for (int i = 0; i < attrs.length; ++i) {
      names[i] = attrs[i].getName();
    }
    return names;
  }

  private Object fixValue(Object obj) {
    if (obj instanceof CompositeData) {
      CompositeData cd = (CompositeData) obj;
      Map<String, Object> map = new TreeMap<>();
      for (String k : cd.getCompositeType().keySet()) {
        map.put(k, fixValue(cd.get(k)));
      }
      return map;
    } else if (obj instanceof TabularData) {
      TabularData td = (TabularData) obj;
      return td.values()
          .stream()
          .map(this::fixValue)
          .collect(Collectors.toList());
    } else if (obj != null && obj.getClass().isArray()) {
      List<Object> vs = new ArrayList<>();
      int len = Array.getLength(obj);
      for (int i = 0; i < len; ++i) {
        vs.add(fixValue(Array.get(obj, i)));
      }
      return vs;
    } else if (obj instanceof Number) {
      return obj;
    } else {
      return "" + obj;
    }
  }

  /**
   * Identifier for an MBean. Based on {@link ObjectName}, but ensures we don't get a lot of
   * extraneous fields in the encoded output.
   */
  public static class JmxId {

    public static JmxId create(ObjectName name) {
      return new JmxId(name.getDomain(), new TreeMap<>(name.getKeyPropertyList()));
    }

    private final String name;
    private final Map<String, String> props;

    public JmxId(String name, Map<String, String> props) {
      this.name = name;
      this.props = props;
    }

    public String getName() {
      return name;
    }

    public Map<String, String> getProps() {
      return props;
    }
  }

  /**
   * Summary of an MBean using simle POJOs, Maps, and Lists.
   */
  public static class JmxBean {
    private final JmxId id;
    private final Map<String, Object> attributes;

    public JmxBean(JmxId id, Map<String, Object> attributes) {
      this.id = id;
      this.attributes = attributes;
    }

    public JmxId getId() {
      return id;
    }

    public Map<String, Object> getAttributes() {
      return attributes;
    }
  }
}
