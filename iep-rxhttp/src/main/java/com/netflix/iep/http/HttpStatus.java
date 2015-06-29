package com.netflix.iep.http;

import io.netty.handler.codec.http.HttpResponseStatus;

public final class HttpStatus {
  private final int code;
  private final String reason;

  public HttpStatus(int code, String reason) {
    this.code = code;
    this.reason = reason;
  }

  public int code() {
    return code;
  }

  public String reason() {
    return reason;
  }

  public static HttpStatus create(HttpResponseStatus s) {
    return new HttpStatus(s.code(), s.reasonPhrase());
  }
}
