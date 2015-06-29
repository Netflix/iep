package com.netflix.iep.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import rx.Observable;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.List;

public abstract class HttpMessage<T extends HttpMessage<T>> {

  protected final SList<HttpHeader> headers;
  protected final Observable<ByteBuf> content;

  HttpMessage(SList<HttpHeader> headers, Observable<ByteBuf> content) {
    this.headers = headers;
    this.content = content;
  }

  public Iterable<HttpHeader> headers() {
    return headers;
  }

  public Observable<ByteBuf> content() {
    return content;
  }

  public long getContentLength() {
    final HttpHeader h = getHeader("content-length");
    return (h == null) ? -1L : h.longValue();
  }

  public T addHeader(HttpHeader h) {
    return withHeaders(headers.prepend(h));
  }

  public T addHeader(String name, String value) {
    return withHeaders(headers.prepend(new HttpHeader(name, value)));
  }

  public HttpHeader getHeader(String name) {
    final String lowerName = name.toLowerCase();
    for (HttpHeader h : headers) {
      if (lowerName.equals(h.lowerName())) {
        return h;
      }
    }
    return null;
  }

  public boolean containsHeader(String name) {
    return getHeader(name) != null;
  }

  public T withContent(String contentType, String data) {
    return withContent(contentType, getBytes(data));
  }

  public T withContent(String contentType, byte[] data) {
    final HttpHeader ctype = new HttpHeader(HttpHeaderNames.CONTENT_TYPE, contentType);
    final HttpHeader clength = new HttpHeader(HttpHeaderNames.CONTENT_LENGTH, "" + data.length);
    final ByteBuf buf = Unpooled.wrappedBuffer(data);
    return addHeader(ctype).addHeader(clength).withContent(Observable.just(buf));
  }

  public abstract T withHeaders(Iterable<HttpHeader> hs);

  public abstract T withContent(Observable<ByteBuf> obs);

  /** We expect UTF-8 to always be supported. */
  static byte[] getBytes(String s) {
    try {
      return s.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}
