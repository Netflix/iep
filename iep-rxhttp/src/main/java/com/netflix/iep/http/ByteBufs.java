package com.netflix.iep.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.compression.JdkZlibDecoder;
import io.netty.handler.codec.compression.JdkZlibEncoder;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import io.reactivex.netty.protocol.http.sse.client.ServerSentEventDecoder;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.functions.Func2;

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
        input.subscribe(new DecoderSubscriber<T>(subscriber, channel));
      }
    });
  }

  /**
   * Map to a GZIP compressed sequence of ByteBuf.
   */
  public static Observable.Transformer<ByteBuf, ByteBuf> gzip() {
    return new Observable.Transformer<ByteBuf, ByteBuf>() {
      @Override public Observable<ByteBuf> call(Observable<ByteBuf> input) {
        EmbeddedChannel channel = new EmbeddedChannel(new JdkZlibEncoder(ZlibWrapper.GZIP));
        return encode(input, channel);
      }
    };
  }

  public static Observable.Transformer<ByteBuf, ByteBuf> gunzip() {
    return new Observable.Transformer<ByteBuf, ByteBuf>() {
      @Override public Observable<ByteBuf> call(Observable<ByteBuf> input) {
        EmbeddedChannel channel = new EmbeddedChannel(new JdkZlibDecoder(ZlibWrapper.GZIP));
        return decode(input, channel);
      }
    };
  }

  public static Observable.Transformer<ByteBuf, ByteBuf> json() {
    return new Observable.Transformer<ByteBuf, ByteBuf>() {
      @Override public Observable<ByteBuf> call(Observable<ByteBuf> input) {
        EmbeddedChannel channel = new EmbeddedChannel(new JsonObjectDecoder(true));
        return decode(input, channel);
      }
    };
  }

  public static Func1<ByteBuf, byte[]> toByteArray() {
    return new Func1<ByteBuf, byte[]>() {
      @Override public byte[] call(ByteBuf bufs) {
        byte[] output = new byte[bufs.readableBytes()];
        bufs.readBytes(output, 0, bufs.readableBytes());
        return output;
      }
    };
  }

  public static Observable.Transformer<ByteBuf, byte[]> aggrByteArray() {
    return new Observable.Transformer<ByteBuf, byte[]>() {
      @Override public Observable<byte[]> call(Observable<ByteBuf> input) {
        return input
            .reduce(Unpooled.compositeBuffer(), new Func2<CompositeByteBuf, ByteBuf, CompositeByteBuf>() {
              @Override public CompositeByteBuf call(CompositeByteBuf bufs, ByteBuf buf) {
                bufs.addComponent(buf);
                bufs.writerIndex(bufs.writerIndex() + buf.readableBytes());
                return bufs;
              }
            })
            .map(toByteArray());
      }
    };
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
