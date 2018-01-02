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
package com.netflix.iep.config;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.Comparator;
import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import javax.script.SimpleBindings;
import javax.script.ScriptException;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * Helpers for loading configuration properties with scoped blocks.
 *
 * <pre>
 * netflix.atlas.foo=1
 *
 * # Defines a scope, properties after this will only be applied if the condition is true
 * # scope: region == "us-east-1" &amp;&amp; stack == "main"
 * netflix.atlas.foo=1
 *
 * Properties can be deleted by setting them to null
 * netflix.atlas.foo=null
 * </pre>
 */
public class ConfigFile {

  public static boolean checkScope(Map<String,String> vars, String str) {
    ScriptEngineManager mgr = new ScriptEngineManager(null);
    ScriptEngine engine = mgr.getEngineByName("javascript");

    if (engine == null) throw new IllegalStateException("no javascipt engine found");

    SimpleBindings bindings = new SimpleBindings();
    for (Map.Entry<String,String> t : vars.entrySet()) {
      bindings.put(t.getKey(), t.getValue());
    }
    try {
      return (Boolean) engine.eval(str, bindings);
    }
    catch (ScriptException e) {
      throw new RuntimeException(e);
    }
  }

  /** Load the configuration file using the system environment variables as the `vars`. */
  public static Map<String,String> loadUsingEnv(File file) {
    return load(System.getenv(), file);
  }

  /** Load the configuration file using the system environment variables as the `vars`. */
  public static Map<String,String> loadUsingEnv(String str) {
    return load(System.getenv(), str);
  }

  /** Load the configuration file using the system environment variables as the `vars`. */
  public static Properties loadPropertiesUsingEnv(File file) {
    return loadProperties(System.getenv(), file);
  }

  /** Load the configuration file using the system environment variables as the `vars`. */
  public static Properties loadPropertiesUsingEnv(String str) {
    return loadProperties(System.getenv(), str);
  }

  public static Map<String,String> load(Map<String,String> vars, File file) {
    try {
      return load(vars, Files.toString(file, Charsets.UTF_8));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Map<String,String> load(Map<String,String> vars, String str) {
    Properties p = loadProperties(vars, str);
    Map<String, String> m = new HashMap<>();
    for (String n : p.stringPropertyNames()) {
      m.put(n, p.getProperty(n));
    }
    return m;
  }

  public static Properties loadProperties(Map<String,String> vars, File file) {
    try {
      return loadProperties(vars, Files.toString(file, Charsets.UTF_8));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Properties loadProperties(Map<String,String> vars, String str) {
    String propStr = toPropertiesString(vars, str);
    Properties p = new Properties();
    try (Reader r = new StringReader(propStr)) {
      p.load(r);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return p;
  }

  public static String toPropertiesString(Map<String,String> vars, File file) {
    try {
      return toPropertiesString(vars, Files.toString(file, Charsets.UTF_8));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Creates a standard java properties string with out of scope properties removed. */
  public static String toPropertiesString(Map<String,String> vars, String str) {
    List<ConfigLine> lines = parse(vars, str);
    List<ConfigLine> inScope = applyOverrides(filterByScope(lines));
    List<String> varList = new ArrayList<>();
    Set<String> sortedNames = new TreeSet<>(vars.keySet());
    for (String k : sortedNames) {
      varList.add(k + " = [" + vars.get(k) + "]");
    }
    String varHeader = mkCommentString(varList, "vars", "\n\n");
    StringBuilder sb = new StringBuilder(varHeader);
    for (ConfigLine cl : inScope) {
      sb.append(cl.getLine()).append("\n");
    }
    return sb.toString();
  }

  private static List<ConfigLine> applyOverrides(List<ConfigLine> lines) {
    List<PropertyLine> props = new ArrayList<>();
    List<ConfigLine> others = new ArrayList<>();
    for (ConfigLine cl : lines) {
      if (cl.isProperty) {
        props.add((PropertyLine)cl);
      }
      else others.add(cl);
    }

    Comparator<ConfigLine> clComp = (p1, p2) -> p1.pos - p2.pos;

    Map<String,TreeSet<PropertyLine>> groupedProps = new HashMap<>();
    for (PropertyLine p : props) {
      TreeSet<PropertyLine> pls = groupedProps.get(p.name);
      if (pls == null) {
        pls = new TreeSet<>(clComp);
        groupedProps.put(p.name, pls);
      }
      pls.add(p);
    }

    TreeSet<ConfigLine> finalProps = new TreeSet<>(clComp);
    for (Map.Entry<String,TreeSet<PropertyLine>> e : groupedProps.entrySet()) {
      NavigableSet<PropertyLine> pls = e.getValue().descendingSet();
      PropertyLine pl = pls.pollFirst();
      List<String> vs = new ArrayList<>();
      for (PropertyLine p : pls) {
        vs.add("[" + p.getScope().expr + "] => [" + p.value + "]");
      }
      finalProps.add(pl.withOverrides(vs));
    }
    for (ConfigLine cl : others) {
      finalProps.add(cl);
    }

    return new ArrayList<>(finalProps);
  }

  private static List<ConfigLine> filterByScope(List<ConfigLine> lines) {
    boolean acc1 = true;
    List<ConfigLine> acc2 = new ArrayList<>();
    for (ConfigLine cl : lines) {
      if (cl instanceof Scope) {
        acc1 = ((Scope)cl).matches;
        acc2.add(0, cl);
      }
      else if (acc1) {
        acc2.add(0, cl);
      }
    }
    return acc2;
  }

  private static List<ConfigLine> parse(Map<String,String> vars, String str) {
    List<ConfigLine> config = new ArrayList<>();
    String[] lines = str.split("\n");
    Scope scope = new Scope("default", 0, true);
    boolean isContinue = false;
    StringBuilder buffer = new StringBuilder();
    for (int i = 0; i < lines.length; i++) {
      String rawLine = lines[i];
      String line = rawLine.trim();
      if (isContinue) {
        buffer.append("\n").append(rawLine);
        if (!line.endsWith("\\")) {
          config.add(mkProperty(buffer.toString(), i, scope));
          buffer =  new StringBuilder();
        }
      }
      else if (line.startsWith("# scope:")) {
        String expr = line.substring("# scope:".length()).trim();
        scope = new Scope(expr, i, checkScope(vars, expr));
        config.add(scope);
      }
      else if (line.startsWith("#")) {
        config.add(new Comment(line, i, scope));
      }
      else if (line.length() == 0) {
        config.add(new EmptyLine(line, i, scope));
      }
      else if (line.endsWith("\\")) {
        isContinue = true;
        buffer.append(line);
      }
      else {
        config.add(mkProperty(line, i, scope));
      }
    }
    return config;
  }

  private static String mkCommentString(List<String> seq, String label, String end) {
    String sep = "\n# --> ";
    StringBuilder sb = new StringBuilder("# ").append(label).append(":");
    for (Object o : seq) {
      sb.append(sep).append(o.toString());
    }
    sb.append(end);
    return sb.toString();
  }

  private static ConfigLine mkProperty(String line, int pos, Scope scope) {
    int eqPos = line.indexOf("=");
    if (eqPos < 1 || eqPos > line.length() - 2)
      throw new IllegalStateException("invalid property line: [" + line + "]");

    String key = line.substring(0, eqPos).trim();
    String value = line.substring(eqPos + 1);
    if (value.equals("null")) return new Delete(key, pos, scope);
    else return new Property(key, value, pos, scope);
  }

  private static class ConfigLine {
    String line;
    int pos;
    Scope scope;
    boolean isProperty = false;

    String getLine() { return line; }
    int getPos() { return pos; }
    Scope getScope() { return scope; }
  }

  private static class PropertyLine extends ConfigLine {
    String name;
    String value;
    List<String> overrides;

    PropertyLine() {
      isProperty = true;
    }

    String lineString() { throw new UnsupportedOperationException(); }
    PropertyLine withOverrides(List<String> overrides) { throw new UnsupportedOperationException(); }

    @Override
    String getLine() {
      if (overrides == null || overrides.size() == 0)
        return lineString();
      else {
        List<String> r = new ArrayList<>();
        for (int i = overrides.size(); i > 0; i--) {
          r.add(overrides.get(i - 1));
        }
        return mkCommentString(r, "overrides", "\n" + lineString());
      }
    }
  }

  private static class Scope extends ConfigLine {
    final String expr;
    final boolean matches;

    Scope(String expr, int pos, boolean matches) {
      super();
      this.expr = expr;
      this.pos = pos;
      this.matches = matches;
    }

    @Override String getLine() { return "# scope: " + expr + " [" + matches + "]"; }
    @Override Scope getScope() { return this; }
  }

  private static class Property extends PropertyLine {

    Property(String name, String value, int pos, Scope scope) {
      this(name, value, pos, scope, null);
    }

    Property(String name, String value, int pos, Scope scope, List<String> overrides) {
      super();
      this.name = name;
      this.value = value;
      this.pos = pos;
      this.scope = scope;
      this.overrides = overrides;
    }

    @Override String lineString() {
      return name + "=" + value;
    }
    @Override PropertyLine withOverrides(List<String> vs) {
      return new Property(name, value, pos, scope, vs);
    }
  }

  private static class Delete extends PropertyLine {
    Delete(String name, int pos, Scope scope) {
      this(name, pos, scope, null);
    }

   Delete(String name, int pos, Scope scope, List<String> overrides) {
      super();
      this.name = name;
      this.value = null;
      this.pos = pos;
      this.scope = scope;
      this.overrides = overrides;
    }

    @Override String lineString() { return "# deleted: " + name; }
    @Override PropertyLine withOverrides(List<String> vs) {
      return new Delete(name, pos, scope, vs);
    }
  }

  private static class Comment extends ConfigLine {
    Comment(String line, int pos, Scope scope) {
      this.line = line;
      this.pos = pos;
      this.scope = scope;
    }
  }
  private static class EmptyLine extends ConfigLine {
    EmptyLine(String line, int pos, Scope scope) {
      this.line = line;
      this.pos = pos;
      this.scope = scope;
    }
  }
}
