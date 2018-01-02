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
package com.netflix.iep.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.TooLongFrameException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import rx.Observable;
import rx.functions.Action1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
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
    ByteBuf[] bufs = new ByteBuf[7];
    byte[] data0 = "[{\"a\":1},{\"a\":2},{\"a\":3".getBytes("UTF-8");
    byte[] data1 = "},{\"a\":4},{\"a\":5}]".getBytes("UTF-8");
    bufs[0] = Unpooled.wrappedBuffer(data0);
    bufs[1] = Unpooled.wrappedBuffer(data1);
    Observable.just(bufs[0], bufs[1])
    .compose(ByteBufs.json()).toBlocking().forEach(new Action1<ByteBuf>() {
      private int i = 0;
      @Override
      public void call(ByteBuf byteBuf) {
        bufs[2 + i] = byteBuf;
        String obj = byteBuf.toString(Charset.forName("UTF-8"));
        byteBuf.release();
        Assert.assertEquals(String.format("{\"a\":%d}", ++i), obj);
      }
    });
    bufs[0].release();
    bufs[1].release();
    for (int i = 0; i < bufs.length; i++) {
      //System.err.println("bufs[" + i + "]: " + bufs[i].refCnt());
      Assert.assertEquals(0, bufs[i].refCnt());
    }
  }

  @Test
  public void jsonEmpty() throws Exception {
    ByteBuf[] bufs = new ByteBuf[1];
    byte[] data = "[     ]".getBytes("UTF-8");
    bufs[0] = Unpooled.wrappedBuffer(data);
    Observable.just(bufs[0])
    .compose(ByteBufs.json()).toBlocking().forEach(new Action1<ByteBuf>() {
      private int i = 0;
      @Override
      public void call(ByteBuf byteBuf) {
        String obj = byteBuf.toString(Charset.forName("UTF-8"));
        byteBuf.release();
        System.err.println("bytesBuf: '" + obj + "'");
        Assert.assertEquals(String.format("{\"a\":%d}", ++i), obj);
        bufs[1 + i] = byteBuf;
      }
    });
    bufs[0].release();
    for (int i = 0; i < bufs.length; i++) {
      //System.err.println("bufs[" + i + "]: " + bufs[i].refCnt());
      Assert.assertEquals(0, bufs[i].refCnt());
    }
  }

  @Test
  public void sse() throws Exception {
    byte[] data = "event:\ndata: foo\ndata:bar\n\ndata:  baz\n".getBytes("UTF-8");
    List<ServerSentEvent> events = obs(data)
        .compose(ByteBufs.sse(10))
        .reduce(new ArrayList<ServerSentEvent>(), (acc, v) -> { acc.add(v); return acc; })
        .toBlocking()
        .single();

    List<ServerSentEvent> expected = new ArrayList<>();
    expected.add(new ServerSentEvent("event", ""));
    expected.add(new ServerSentEvent("data", "foo"));
    expected.add(new ServerSentEvent("data", "bar"));
    expected.add(new ServerSentEvent("data", " baz"));

    Assert.assertEquals(expected, events);
  }

  @Test
  public void linesLF() throws Exception {
    byte[] data = "0\n1\n2\n3\n".getBytes("UTF-8");
    int count = obs(data).compose(ByteBufs.lines(10))
        .reduce(0, (acc, b) -> {
          Assert.assertEquals("" + acc, b.toString(Charset.forName("UTF-8")));
          return acc + 1;
        })
        .toBlocking()
        .single();
    Assert.assertEquals(4, count);
  }

  @Test
  public void linesCRLF() throws Exception {
    byte[] data = "0\r\n1\r\n2\r\n3\r\n".getBytes("UTF-8");
    int count = obs(data).compose(ByteBufs.lines(10))
        .reduce(0, (acc, b) -> {
          Assert.assertEquals("" + acc, b.toString(Charset.forName("UTF-8")));
          return acc + 1;
        })
        .toBlocking()
        .single();
    Assert.assertEquals(4, count);
  }

  @Test
  public void linesCR() throws Exception {
    byte[] data = "0\r1\r2\r3\r".getBytes("UTF-8");
    int count = obs(data).compose(ByteBufs.lines(10))
        .reduce(0, (acc, b) -> {
          Assert.assertEquals("" + acc, b.toString(Charset.forName("UTF-8")));
          return acc + 1;
        })
        .toBlocking()
        .single();
    Assert.assertEquals(0, count);
  }

  // Currently ignores the last bit, not sure how to force it to flush
  @Test
  public void linesNoEndingLF() throws Exception {
    byte[] data = "0\n1\n2\n3".getBytes("UTF-8");
    int count = obs(data).compose(ByteBufs.lines(10))
        .reduce(0, (acc, b) -> {
          System.out.println(b.toString(Charset.forName("UTF-8")));
          Assert.assertEquals("" + acc, b.toString(Charset.forName("UTF-8")));
          return acc + 1;
        })
        .toBlocking()
        .single();
    Assert.assertEquals(3, count);
  }

  @Test
  public void linesEmpty() throws Exception {
    byte[] data = "\n\n\n".getBytes("UTF-8");
    int count = obs(data).compose(ByteBufs.lines(10))
        .reduce(0, (acc, b) -> {
          Assert.assertEquals("", b.toString(Charset.forName("UTF-8")));
          return acc + 1;
        })
        .toBlocking()
        .single();
    Assert.assertEquals(3, count);
  }

  @Test(expected = TooLongFrameException.class)
  public void linesFailure() throws Exception {
    byte[] data = "1\n22\r\n3\r4".getBytes("UTF-8");
    obs(data).compose(ByteBufs.lines(1)).toBlocking().forEach(new Action1<ByteBuf>() {
      private int i = 0;
      @Override
      public void call(ByteBuf byteBuf) {
        String obj = byteBuf.toString(Charset.forName("UTF-8"));
        Assert.assertEquals(String.format("%d", ++i), obj);
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
