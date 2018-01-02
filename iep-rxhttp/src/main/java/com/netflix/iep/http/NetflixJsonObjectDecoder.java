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
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.channel.ChannelPipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Splits a byte stream of JSON objects and arrays into individual objects/arrays and passes them up the
 * {@link ChannelPipeline}.
 *
 * This class does not do any real parsing or validation. A sequence of bytes is considered a JSON object/array
 * if it contains a matching number of opening and closing braces/brackets. It's up to a subsequent
 * {@link ChannelHandler} to parse the JSON text into a more usable form i.e. a POJO.
 */
public class NetflixJsonObjectDecoder extends ByteToMessageDecoder {

    protected static final Logger LOGGER = LoggerFactory.getLogger(NetflixJsonObjectDecoder.class);

    private static final int ST_CORRUPTED = -1;
    private static final int ST_INIT = 0;
    private static final int ST_DECODING_NORMAL = 1;
    private static final int ST_DECODING_ARRAY_STREAM = 2;

    private int openBraces;
    private int len;

    private int state;
    private boolean insideString;
    private boolean escapeString;

    private final int maxObjectLength;
    private final boolean streamArrayElements;

    public NetflixJsonObjectDecoder() {
        // 1 MB
        this(1024 * 1024);
    }

    public NetflixJsonObjectDecoder(int maxObjectLength) {
        this(maxObjectLength, false);
    }

    public NetflixJsonObjectDecoder(boolean streamArrayElements) {
        this(1024 * 1024, streamArrayElements);
    }

    /**
     * @param maxObjectLength   maximum number of bytes a JSON object/array may use (including braces and all).
     *                             Objects exceeding this length are dropped and an {@link TooLongFrameException}
     *                             is thrown.
     * @param streamArrayElements   if set to true and the "top level" JSON object is an array, each of its entries
     *                                  is passed through the pipeline individually and immediately after it was fully
     *                                  received, allowing for arrays with "infinitely" many elements.
     *
     */
    public NetflixJsonObjectDecoder(int maxObjectLength, boolean streamArrayElements) {
        if (maxObjectLength < 1) {
            throw new IllegalArgumentException("maxObjectLength must be a positive int");
        }
        this.maxObjectLength = maxObjectLength;
        this.streamArrayElements = streamArrayElements;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (state == ST_CORRUPTED) {
            in.skipBytes(in.readableBytes());
            return;
        }

        if (LOGGER.isTraceEnabled()) {
          byte[] bytes = new byte[in.readableBytes()];
          in.getBytes(in.readerIndex(), bytes, 0, in.readableBytes());
          LOGGER.trace("starting [" + in.readerIndex() + ":" + in.readableBytes() + "]:" + new String(bytes));
        }

        // index of next byte to process.
        int len = this.len;
        int wrtIdx = in.writerIndex();

        for (; in.readerIndex() + len < wrtIdx; len++) {
            if (len > maxObjectLength) {
                // buffer size exceeded maxObjectLength; discarding the complete buffer.
                in.skipBytes(in.readableBytes());
                reset();
                throw new TooLongFrameException(
                                "object length exceeds " + maxObjectLength + ": " + len + " bytes discarded");
            }
            byte c = in.getByte(in.readerIndex() + len);
            if (state == ST_DECODING_NORMAL) {
                decodeByte(c, in, in.readerIndex() + len);

                // All opening braces/brackets have been closed. That's enough to conclude
                // that the JSON object/array is complete.
                if (openBraces == 0) {
                    ByteBuf json = extractObject(ctx, in, in.readerIndex(), len + 1);
                    if (json != null) {
                        out.add(json);
                    }

                    // The JSON object/array was extracted => discard the bytes from
                    // the input buffer.
                    in.readerIndex(in.readerIndex() + len + 1);
                    len = 0;
                    // Reset the object state to get ready for the next JSON object/text
                    // coming along the byte stream.
                    reset();
                    break;
                }
            } else if (state == ST_DECODING_ARRAY_STREAM) {
                if (len == 0 && Character.isWhitespace(c)) {
                    in.skipBytes(1);
                    len--;
                }
                decodeByte(c, in, in.readerIndex() + len);
                if (!insideString && (openBraces == 1 && c == ',' || openBraces == 0 && c == ']')) {
                    ByteBuf json = extractObject(ctx, in, in.readerIndex(), len);
                    if (json != null) {
                        out.add(json);
                    }

                    in.readerIndex(in.readerIndex() + len + 1);
                    len = 0;

                    if (c == ']') {
                        reset();
                    }
                    break;
                }
            // JSON object/array detected. Accumulate bytes until all braces/brackets are closed.
            } else if (c == '{' || c == '[') {
                initDecoding(c);

                if (state == ST_DECODING_ARRAY_STREAM) {
                    // Discard the array bracket
                    in.skipBytes(1);
                    len--;
                }
            // Discard leading spaces in front of a JSON object/array.
            } else if (Character.isWhitespace(c)) {
                in.skipBytes(1);
                len--;
            } else {
                state = ST_CORRUPTED;
                throw new CorruptedFrameException(
                        "invalid JSON received at byte position " + (in.readerIndex() + len) + ": " + ByteBufUtil.hexDump(in));
            }
        }

        this.len = len;

        if (LOGGER.isTraceEnabled()) {
            byte[] bytes = new byte[in.readableBytes()];
            in.getBytes(in.readerIndex(), bytes, 0, in.readableBytes());
            LOGGER.trace("remainder [" + in.readerIndex() + ":" + in.readableBytes() + "]:" + new String(bytes));
        }
    }

    /**
     * Override this method if you want to filter the json objects/arrays that get passed through the pipeline.
     */
    @SuppressWarnings("UnusedParameters")
    protected ByteBuf extractObject(ChannelHandlerContext ctx, ByteBuf buffer, int index, int length) {
        if (length == 0) return null;
        ByteBuf buf = buffer.slice(index, length).retain();

        if (LOGGER.isTraceEnabled()) {
            byte[] bytes = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(), bytes, 0, buf.readableBytes());
            LOGGER.trace("extracted [" + buf.readerIndex() + ":" + buf.readableBytes() + "]:" + new String(bytes));
        }

        return buf;
    }

    private void decodeByte(byte c, ByteBuf in, int idx) {
        if ((c == '{' || c == '[') && !insideString) {
            openBraces++;
        } else if ((c == '}' || c == ']') && !insideString) {
            openBraces--;
        } else if (c == '"') {
            // start of a new JSON string. It's necessary to detect strings as they may
            // also contain braces/brackets and that could lead to incorrect results.
            if (!insideString) {
                insideString = true;
            // If the double quote wasn't escaped then this is the end of a string.
            } else if (!escapeString) {
                insideString = false;
            }
        }

        escapeString = insideString && c == '\\' && !escapeString;
    }

    private void initDecoding(byte openingBrace) {
        openBraces = 1;
        if (openingBrace == '[' && streamArrayElements) {
            state = ST_DECODING_ARRAY_STREAM;
        } else {
            state = ST_DECODING_NORMAL;
        }
    }

    private void reset() {
        insideString = false;
        escapeString = false;
        state = ST_INIT;
        openBraces = 0;
    }
}
