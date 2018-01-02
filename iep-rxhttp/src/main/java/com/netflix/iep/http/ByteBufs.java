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
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.compression.JdkZlibDecoder;
import io.netty.handler.codec.compression.JdkZlibEncoder;
import io.netty.handler.codec.compression.ZlibWrapper;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.functions.Func2;

import java.io.UnsupportedEncodingException;

/**
 * Helper operations for working with {@code Observable<ByteBuf>} objects.
 */
public final class ByteBufs {

  private ByteBufs() {
  }

  private static Observable<ByteBuf> encode(final Observable<ByteBuf> input, final EmbeddedChannel channel) {
    return Observable.create(new Observable.OnSubscribe<ByteBuf>() {
      @Override public void call(final Subscriber<? super ByteBuf> subscriber) {
        input.subscribe(new EncoderSubscriber(subscriber, channel));
      }
    });
  }

  private static <T> Observable<T> decode(final Observable<ByteBuf> input, final EmbeddedChannel channel) {
    return Observable.create(new Observable.OnSubscribe<T>() {
      @Override public void call(final Subscriber<? super T> subscriber) {
        input.subscribe(new DecoderSubscriber<>(subscriber, channel));
      }
    });
  }

  /**
   * Map to a GZIP compressed sequence of ByteBuf.
   */
  public static Observable.Transformer<ByteBuf, ByteBuf> gzip() {
    return input -> encode(input, new EmbeddedChannel(new JdkZlibEncoder(ZlibWrapper.GZIP)));
  }

  /**
   * Map a GZIP compressed sequence of ByteBufs to a decompressed sequence.
   */
  public static Observable.Transformer<ByteBuf, ByteBuf> gunzip() {
    return input -> decode(input, new EmbeddedChannel(new JdkZlibDecoder(ZlibWrapper.GZIP)));
  }

  /**
   * Process json data encoded as a sequence of ByteBufs so that each object within an array
   * will be a single ByteBuf in the output.
   */
  public static Observable.Transformer<ByteBuf, ByteBuf> json() {
    return json(1024 * 1024);
  }
  public static Observable.Transformer<ByteBuf, ByteBuf> json(int maxLength) {
    return input -> decode(autoReleaseCopy(input), new EmbeddedChannel(new NetflixJsonObjectDecoder(maxLength, true)));
  }

  /**
   * Create copy of bytebuf to prevent underlying creator from mucking with contents.
   */
  public static Observable<ByteBuf> autoReleaseCopy(Observable<ByteBuf> stream) {
    return stream.map(in -> {
      ByteBuf buf = io.netty.buffer.ByteBufAllocator.DEFAULT.buffer(in.readableBytes());
      buf.writeBytes(in);
      return buf;
    });
  }

  /**
   * Process text data so that each output ByteBuf is a single line. The final line must have a
   * trailing line feed or it will be skipped.
   *
   * @param maxLength
   *     Maximum length of a line. If a line that is too long is reached a TooLongFrameException
   *     will be thrown.
   * @return
   *     Observable where each ByteBuf is a single line.
   */
  public static Observable.Transformer<ByteBuf, ByteBuf> lines(int maxLength) {
    return input -> {
      LineBasedFrameDecoder decoder = new LineBasedFrameDecoder(maxLength, true, true);
      return decode(input, new EmbeddedChannel(decoder));
    };
  }

  /**
   * Map a sequence of ByteBufs to a sequence of ServerSentEvent objects.
   *
   * @param maxLength
   *     Maximum length of a line. If a line that is too long is reached a TooLongFrameException
   *     will be thrown.
   * @return
   *     Observable of server sent events.
   */
  public static Observable.Transformer<ByteBuf, ServerSentEvent> sse(int maxLength) {
    return input -> input
        .compose(lines(maxLength))
        .map(ServerSentEvent::parse)
        .filter(v -> v != null);
  }

  /**
   * Map a ByteBuf to a byte array.
   */
  public static Func1<ByteBuf, byte[]> toByteArray() {
    return bufs -> {
      byte[] output = new byte[bufs.readableBytes()];
      bufs.readBytes(output, 0, bufs.readableBytes());
      return output;
    };
  }

  private static Func2<CompositeByteBuf, ByteBuf, CompositeByteBuf> append() {
    return (bufs, buf) -> {
      bufs.addComponent(buf);
      bufs.writerIndex(bufs.writerIndex() + buf.readableBytes());
      return bufs;
    };
  }

  /**
   * Create an aggregated byte array with all data from the ByteBufs in the input observable.
   */
  public static Observable.Transformer<ByteBuf, byte[]> aggrByteArray() {
    return input -> input.reduce(Unpooled.compositeBuffer(), append()).map(toByteArray());
  }

  private static String newString(byte[] buf, String enc) {
    try {
      return new String(buf, enc);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Create a string using the aggregated content from the ByteBufs in the input observable.
   */
  public static Observable.Transformer<ByteBuf, String> toString(String enc) {
    return input -> input.compose(aggrByteArray()).map(buf -> newString(buf, enc));
  }

  private static class EncoderSubscriber extends Subscriber<ByteBuf> {

    private final Subscriber<? super ByteBuf> consumer;
    private final EmbeddedChannel channel;

    public EncoderSubscriber(Subscriber<? super ByteBuf> consumer, EmbeddedChannel channel) {
      this.consumer = consumer;
      this.channel = channel;
    }

    @Override
    public void onNext(ByteBuf buf) {
      channel.writeOutbound(buf);
      ByteBuf msg;
      while ((msg = channel.readOutbound()) != null) {
        consumer.onNext(msg);
      }
    }

    @Override
    public void onCompleted() {
      channel.finish();
      ByteBuf msg;
      while ((msg = channel.readOutbound()) != null) {
        consumer.onNext(msg);
      }
      consumer.onCompleted();
    }

    @Override
    public void onError(Throwable throwable) {
      consumer.onError(throwable);
    }
  }

  private static class DecoderSubscriber<T> extends Subscriber<ByteBuf> {

    private final Subscriber<? super T> consumer;
    private final EmbeddedChannel channel;

    public DecoderSubscriber(Subscriber<? super T> consumer, EmbeddedChannel channel) {
      this.consumer = consumer;
      this.channel = channel;
    }

    @Override
    public void onNext(ByteBuf buf) {
      channel.writeInbound(buf);
      T msg;
      while ((msg = channel.readInbound()) != null) {
        consumer.onNext(msg);
      }
    }

    @Override
    public void onCompleted() {
      channel.finish();
      T msg;
      while ((msg = channel.readInbound()) != null) {
        consumer.onNext(msg);
      }
      consumer.onCompleted();
    }

    @Override
    public void onError(Throwable throwable) {
      consumer.onError(throwable);
    }
  }
}
