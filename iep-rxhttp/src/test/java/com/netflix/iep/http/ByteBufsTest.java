/*
 * Copyright 2015 Netflix, Inc.
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
package com.netflix.iep.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import rx.Observable;
import rx.functions.Action1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;

@RunWith(JUnit4.class)
public class ByteBufsTest {

  private byte[] gzipDecompress(byte[] data) throws Exception {
    ByteArrayInputStream bais = new ByteArrayInputStream(data);
    try (GZIPInputStream in = new GZIPInputStream(bais)) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      byte[] buf = new byte[4096];
      int length;
      while ((length = in.read(buf)) > 0) {
        baos.write(buf, 0, length);
      }
      return baos.toByteArray();
    }
  }

  private Observable<ByteBuf> obs(byte[] data) {
    return Observable.just(Unpooled.wrappedBuffer(data));
  }

  @Test
  public void gzip() throws Exception {
    byte[] data = "foo".getBytes("UTF-8");
    byte[] compressed = obs(data)
        .compose(ByteBufs.gzip())
        .compose(ByteBufs.aggrByteArray())
        .toBlocking().first();
    Assert.assertArrayEquals(data, gzipDecompress(compressed));
  }

  @Test
  public void gunzip() throws Exception {
    byte[] data = "foo".getBytes("UTF-8");
    byte[] decompressed = obs(data)
        .compose(ByteBufs.gzip())
        .compose(ByteBufs.gunzip())
        .compose(ByteBufs.aggrByteArray())
        .toBlocking().first();
    Assert.assertArrayEquals(data, decompressed);
  }

  @Test
  public void json() throws Exception {
    byte[] data = "[{\"a\":1},{\"a\":2},{\"a\":3},{\"a\":4},{\"a\":5}]".getBytes("UTF-8");
    obs(data).compose(ByteBufs.json()).toBlocking().forEach(new Action1<ByteBuf>() {
      private int i = 0;
      @Override
      public void call(ByteBuf byteBuf) {
        String obj = byteBuf.toString(Charset.forName("UTF-8"));
        Assert.assertEquals(String.format("{\"a\":%d}", ++i), obj);
      }
    });
  }

  @Test
  public void bufToString() throws Exception {
    String result = Observable.merge(
          obs("foo".getBytes("UTF-8")),
          obs("bar".getBytes("UTF-8")),
          obs("baz".getBytes("UTF-8")))
        .compose(ByteBufs.toString("UTF-8"))
        .toBlocking()
        .first();
    Assert.assertEquals("foobarbaz", result);
  }
}