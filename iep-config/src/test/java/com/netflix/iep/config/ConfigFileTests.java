package com.netflix.iep.config;

import java.util.Map;
import java.util.HashMap;

import static org.junit.Assert.*;
import org.junit.Test;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.netflix.config.ConfigurationManager;
import org.apache.commons.configuration.AbstractConfiguration;

public class ConfigFileTests {

  Map<String, String> envs() {
    return new HashMap<String,String>() {{
      put("a", "b");
      put("c", "d");
    }};
  }

  String propString(String config) {
    return propString(envs(), config);
  }

  String propString(Map<String,String> vars, String config) {
    String s = ConfigFile.toPropertiesString(vars, config);
    StringBuilder buf = new StringBuilder();
    String[] lines = s.split("\n");
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      if (line.length() == 0) buf.append("\n");
      else buf.append("\n      ").append(line);
    }
    return buf.append("\n    ").toString();
  }

  @Test
  public void scopeCondition() {
    Map<String, String> map = new HashMap<String,String>() {{
      put("a", "b");
    }};
    assertTrue(ConfigFile.checkScope(map, "a == 'b'"));
    assertTrue(!ConfigFile.checkScope(map, "a != 'b'"));
  }

  @Test
  public void toPropertiesStringBasicOverride() {
    String config = new StringBuilder()
      .append("      a.b.c=foo\n")
      .append("      a.b.c=bar\n")
      .toString().trim();

    String expected = new StringBuilder()
      .append("\n")
      .append("      # vars:\n")
      .append("      # --> a = [b]\n")
      .append("      # --> c = [d]\n")
      .append("\n")
      .append("      # overrides:\n")
      .append("      # --> [default] => [foo]\n")
      .append("      a.b.c=bar\n")
      .append("    ")
      .toString();

    assertEquals(expected, propString(config));
  }

  @Test
  public void toPropertiesStringBasicDelete() {
    String config = new StringBuilder()
      .append("      a.b.c=foo\n")
      .append("      a.b.c=null\n")
      .toString().trim();

    String expected = new StringBuilder()
      .append("\n")
      .append("      # vars:\n")
      .append("      # --> a = [b]\n")
      .append("      # --> c = [d]\n")
      .append("\n")
      .append("      # overrides:\n")
      .append("      # --> [default] => [foo]\n")
      .append("      # deleted: a.b.c\n")
      .append("    ")
      .toString();

    assertEquals(expected, propString(config));
  }

  @Test
  public void toPropertiesStringScopedOverride() {
    String config = new StringBuilder()
      .append("      a.b.c=foo\n")
      .append("      # scope: a == \"b\"\n")
      .append("      a.b.c=bar\n")
      .toString().trim();

    String expected = new StringBuilder()
      .append("\n")
      .append("      # vars:\n")
      .append("      # --> a = [b]\n")
      .append("      # --> c = [d]\n")
      .append("\n")
      .append("      # scope: a == \"b\" [true]\n")
      .append("      # overrides:\n")
      .append("      # --> [default] => [foo]\n")
      .append("      a.b.c=bar\n")
      .append("    ")
      .toString();

    assertEquals(expected, propString(config));
  }

  @Test
  public void toPropertiesStringScopeIgnored() {
    String config = new StringBuilder()
      .append("      a.b.c=foo\n")
      .append("      # scope: a == \"b\" && c == \"f\"\n")
      .append("      a.b.c=bar\n")
      .toString().trim();

    String expected = new StringBuilder()
      .append("\n")
      .append("      # vars:\n")
      .append("      # --> a = [b]\n")
      .append("      # --> c = [d]\n")
      .append("\n")
      .append("      a.b.c=foo\n")
      .append("      # scope: a == \"b\" && c == \"f\" [false]\n")
      .append("    ")
      .toString();

    assertEquals(expected, propString(config));
  }

  @Test
  public void toPropertiesStringManyScopes() {
    String config = new StringBuilder()
.append("      a.b.c=foo\n")
.append("      # Something useful that will be missing context due to override, might fix someday\n")
.append("      a.b.d=1\n")
.append("      a.b.z=42\n")
.append("\n")
.append("      # scope: a == \"b\"\n")
.append("      a.b.c=bar\n")
.append("      a.b.e=4\n")
.append("      a.b.f=5\n")
.append("\n")
.append("      # scope: c == \"d\"\n")
.append("      a.b.c=null\n")
.append("      a.b.d=2\n")
.append("\n")
.append("      # scope: a == \"f\" && c == \"d\"\n")
.append("      a.b.c=and\n")
.append("      a.b.f=6\n")
.append("\n")
.append("      # scope: a == \"f\" || c == \"d\"\n")
.append("      a.b.c=or\n")
.append("\n")
.append("      # A helpful comment that should be preserved\n")
.append("      a.b.f=7\n")
      .toString().trim();

    String expected = new StringBuilder("\n")
.append("      # vars:\n")
.append("      # --> a = [b]\n")
.append("      # --> c = [d]\n")
.append("\n")
.append("      # Something useful that will be missing context due to override, might fix someday\n")
.append("      a.b.z=42\n")
.append("\n")
.append("      # scope: a == \"b\" [true]\n")
.append("      a.b.e=4\n")
.append("\n")
.append("      # scope: c == \"d\" [true]\n")
.append("      # overrides:\n")
.append("      # --> [default] => [1]\n")
.append("      a.b.d=2\n")
.append("\n")
.append("      # scope: a == \"f\" && c == \"d\" [false]\n")
.append("      # scope: a == \"f\" || c == \"d\" [true]\n")
.append("      # overrides:\n")
.append("      # --> [default] => [foo]\n")
.append("      # --> [a == \"b\"] => [bar]\n")
.append("      # --> [c == \"d\"] => [null]\n")
.append("      a.b.c=or\n")
.append("\n")
.append("      # A helpful comment that should be preserved\n")
.append("      # overrides:\n")
.append("      # --> [a == \"b\"] => [5]\n")
.append("      a.b.f=7\n")
.append("    ")
    .toString();

    assertEquals(expected, propString(config));
  }

  @Test
  public void load() {
    String config = new StringBuilder()
.append("      a.b.c=foo\n")
.append("      # Something useful that will be missing context due to override, might fix someday\n")
.append("      a.b.d=1\n")
.append("      a.b.z=42\n")
.append("\n")
.append("      # scope: a == \"b\"\n")
.append("      a.b.c=bar\n")
.append("      a.b.e=4\n")
.append("      a.b.f=5\n")
.append("\n")
.append("      # scope: c == \"d\"\n")
.append("      a.b.c=null\n")
.append("      a.b.d=2\n")
.append("\n")
.append("      # scope: a == \"f\" || c == \"d\"\n")
.append("      a.b.c=or\n")
.append("\n")
.append("      # A helpful comment that should be preserved\n")
.append("      a.b.f=7\n")
.append("\n")
.append("      # scope: a == \"f\" && c == \"d\"\n")
.append("      a.b.c=and\n")
.append("      a.b.f=6\n")
    .toString().trim();

    Map<String,String> expected = new HashMap<String,String>() {{
      put("a.b.z", "42");
      put("a.b.e", "4");
      put("a.b.d", "2");
      put("a.b.c", "or");
      put("a.b.f", "7");
    }};

    assertEquals(expected, ConfigFile.load(envs(), config));
  }
}
