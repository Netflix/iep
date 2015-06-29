package com.netflix.iep.http;

import java.util.Map;

public class HttpHeader {
  private final CharSequence name;
  private final CharSequence value;

  public HttpHeader(CharSequence name, CharSequence value) {
    this.name = name;
    this.value = value;
  }

  public CharSequence name() {
    return name;
  }

  public String lowerName() {
    return name().toString();
  }

  public CharSequence value() {
    return value;
  }

  public int intValue() {
    return Integer.parseInt(value().toString());
  }

  public long longValue() {
    return Long.parseLong(value().toString());
  }

  public static HttpHeader create(Map.Entry<? extends CharSequence, ? extends CharSequence> entry) {
    return new HttpHeader(entry.getKey(), entry.getValue());
  }
}
