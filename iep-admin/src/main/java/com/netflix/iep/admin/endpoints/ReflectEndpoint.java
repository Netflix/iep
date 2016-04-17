/*
 * Copyright 2014-2016 Netflix, Inc.
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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Uses reflection to show the state of fields for the provided object.
 */
public class ReflectEndpoint implements HttpEndpoint {

  private static final Pattern INDEX = Pattern.compile("^\\d+$");
  private static final Pattern RANGE = Pattern.compile("^(\\d+)\\.\\.(\\d+)$");

  private final Object obj;

  public ReflectEndpoint(Object obj) {
    this.obj = obj;
  }

  @Override public Object get() {
    return ObjectInfo.create(obj);
  }

  @Override public Object get(String path) {
    String[] parts = path.split("/");
    Object o = get(parts, 0, obj);
    return (o != null && o.getClass().isArray()) ? ArrayInfo.create(o) : ObjectInfo.create(o);
  }

  private Object get(String[] parts, int pos, Object o) {
    if (o == null) return null;
    for (; pos < parts.length; ++pos) {
      String p = parts[pos];
      try {
        Class<?> cls = o.getClass();
        if (cls.isArray()) {
          Matcher m = RANGE.matcher(p);
          if (m.matches()) {
            int len = Array.getLength(o);
            int e = Math.min(Integer.parseInt(m.group(2)), len - 1);
            int s = Math.min(Integer.parseInt(m.group(1)), e);
            if (s > 0 || e < len - 1) {
              Object[] range = new Object[e - s + 1];
              for (int i = s; i <= e && i < len; ++i) {
                range[i - s] = Array.get(o, i);
              }
              o = range;
            }
          } else if (INDEX.matcher(p).matches()) {
            int i = Integer.parseInt(p);
            o = Array.get(o, i);
          } else {
            int len = Array.getLength(o);
            Object[] objs = new Object[len];
            for (int i = 0; i < len; ++i) {
              objs[i] = get(parts, pos, Array.get(o, i));
            }
            return objs;
          }
        } else {
          Field f = cls.getDeclaredField(p);
          f.setAccessible(true);
          o = f.get(o);
        }
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    }
    return o;
  }

  public static class ObjectInfo {

    private static final ObjectInfo NULL = new ObjectInfo("null", Collections.emptyList());

    public static ObjectInfo create(Object obj) {
      if (obj == null) {
        return NULL;
      }
      Class<?> cls = obj.getClass();
      List<FieldInfo> fields = new ArrayList<>();
      for (Field f : cls.getDeclaredFields()) {
        fields.add(FieldInfo.create(obj, f));
      }
      return new ObjectInfo(cls.toGenericString(), fields);
    }

    private final String cls;
    private final List<FieldInfo> fields;

    public ObjectInfo(String cls, List<FieldInfo> fields) {
      this.cls = cls;
      this.fields = fields;
    }

    public String getCls() {
      return cls;
    }

    public List<FieldInfo> getFields() {
      return fields;
    }
  }

  public static class ArrayInfo {

    public static ArrayInfo create(Object obj) {
      Class<?> cls = obj.getClass();
      List<ObjectInfo> items = new ArrayList<>();
      int length = Array.getLength(obj);
      int min = Math.min(length, 10);
      for (int i = 0; i < min; ++i) {
        items.add(ObjectInfo.create(Array.get(obj, i)));
      }
      return new ArrayInfo(cls.getName(), length, items);
    }

    private final String className;
    private final int length;
    private final List<ObjectInfo> items;

    public ArrayInfo(String className, int length, List<ObjectInfo> items) {
      this.className = className;
      this.length = length;
      this.items = items;
    }

    public String getClassName() {
      return className;
    }

    public int getLength() {
      return length;
    }

    public List<ObjectInfo> getItems() {
      return items;
    }
  }

  public static class FieldInfo {

    public static FieldInfo create(Object obj, Field field) {
      field.setAccessible(true);
      String value = null;
      try {
        Object o = field.get(obj);
        value = (o == null) ? "null" : o.toString();
      } catch (Throwable t) {
        value = t.getClass() + ": " + t.getMessage();
      }

      int mod = field.getModifiers();
      Set<String> modifiers = new TreeSet<>();
      if (Modifier.isPublic(mod))    modifiers.add("public");
      if (Modifier.isProtected(mod)) modifiers.add("protected");
      if (Modifier.isPrivate(mod))   modifiers.add("private");
      if (Modifier.isStatic(mod))    modifiers.add("static");
      if (Modifier.isFinal(mod))     modifiers.add("final");

      return new FieldInfo(
          field.getType().getName(),
          field.getType().toGenericString(),
          modifiers,
          field.getName(),
          value);
    }

    private final String cls;
    private final String clsSignature;
    private final Set<String> modifiers;
    private final String name;
    private final String value;

    public FieldInfo(String cls, String clsSignature, Set<String> modifiers, String name, String value) {
      this.cls = cls;
      this.clsSignature = clsSignature;
      this.modifiers = modifiers;
      this.name = name;
      this.value = value;
    }

    public String getCls() {
      return cls;
    }

    public String getClsSignature() {
      return clsSignature;
    }

    public Set<String> getModifiers() {
      return modifiers;
    }

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }
  }
}
