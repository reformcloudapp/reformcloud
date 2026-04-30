/*
 * This file is part of reformcloud, licensed under the MIT License (MIT).
 *
 * Copyright (c) ReformCloud <https://github.com/reformcloudapp>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package app.reformcloud.node.http.websocket.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import app.reformcloud.http.websocket.SocketFrame;
import app.reformcloud.http.websocket.SocketFrameType;
import app.reformcloud.http.websocket.listener.SocketFrameListenerRegistryEntry;
import app.reformcloud.http.websocket.request.RequestFrameHolder;
import app.reformcloud.http.websocket.request.SocketFrameSource;
import app.reformcloud.http.websocket.response.ResponseFrameHolder;
import app.reformcloud.node.http.utils.BinaryUtils;
import app.reformcloud.node.http.websocket.DefaultCloseSocketFrame;
import app.reformcloud.node.http.websocket.DefaultContinuationSocketFrame;
import app.reformcloud.node.http.websocket.DefaultTextSocketFrame;
import app.reformcloud.node.http.websocket.TypedSocketFrame;
import app.reformcloud.node.http.websocket.request.DefaultRequestFrameHolder;

import java.io.IOException;
import java.util.Arrays;

public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final SocketFrameSource socketFrameSource;

    public WebSocketFrameHandler(SocketFrameSource socketFrameSource) {
        this.socketFrameSource = socketFrameSource;
    }

    @NotNull
    private static SocketFrame<?> fromNetty(WebSocketFrame frame) {
        return switch (frame) {
            case TextWebSocketFrame textWebSocketFrame ->
                    new DefaultTextSocketFrame(frame.rsv(), frame.isFinalFragment(), textWebSocketFrame.text());
            case CloseWebSocketFrame closeFrame ->
                    new DefaultCloseSocketFrame(frame.rsv(), frame.isFinalFragment(), closeFrame.statusCode(), closeFrame.reasonText());
            case ContinuationWebSocketFrame continuationWebSocketFrame ->
                    new DefaultContinuationSocketFrame(frame.rsv(), frame.isFinalFragment(), continuationWebSocketFrame.text());
            case PingWebSocketFrame _ ->
                    new TypedSocketFrame(SocketFrameType.PING, frame.rsv(), frame.isFinalFragment(), BinaryUtils.binaryArrayFromByteBuf(frame));
            case PongWebSocketFrame _ ->
                    new TypedSocketFrame(SocketFrameType.PONG, frame.rsv(), frame.isFinalFragment(), BinaryUtils.binaryArrayFromByteBuf(frame));
            case BinaryWebSocketFrame _ ->
                    new TypedSocketFrame(SocketFrameType.BINARY, frame.rsv(), frame.isFinalFragment(), BinaryUtils.binaryArrayFromByteBuf(frame));
            default ->
                    throw new IllegalStateException("Illegal/unimplemented socket frame type: " + frame.getClass().getName());
        };
    }

    @Contract("_ -> new")
    public static @NotNull WebSocketFrame toNetty(@NotNull SocketFrame<?> frame) {
        return switch (frame.type()) {
            case TEXT ->
                    new TextWebSocketFrame(frame.finalFragment(), frame.rsv(), Unpooled.wrappedBuffer(frame.content()));
            case PING ->
                    new PingWebSocketFrame(frame.finalFragment(), frame.rsv(), Unpooled.wrappedBuffer(frame.content()));
            case PONG ->
                    new PongWebSocketFrame(frame.finalFragment(), frame.rsv(), Unpooled.wrappedBuffer(frame.content()));
            case BINARY ->
                    new BinaryWebSocketFrame(frame.finalFragment(), frame.rsv(), Unpooled.wrappedBuffer(frame.content()));
            case CLOSE ->
                    new CloseWebSocketFrame(frame.finalFragment(), frame.rsv(), Unpooled.wrappedBuffer(frame.content()));
            case CONTINUATION ->
                    new ContinuationWebSocketFrame(frame.finalFragment(), frame.rsv(), Unpooled.wrappedBuffer(frame.content()));
        };
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!(cause instanceof IOException)) {
            cause.printStackTrace();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (!ctx.channel().isActive() || !ctx.channel().isOpen() || !ctx.channel().isWritable()) {
            ctx.channel().close();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) {
        RequestFrameHolder frame = new DefaultRequestFrameHolder(fromNetty(msg), this.socketFrameSource);
        for (SocketFrameListenerRegistryEntry listener : this.socketFrameSource.listenerRegistry().getListeners()) {
            if (listener.getHandlingFrameTypes().length != 0 && Arrays.binarySearch(listener.getHandlingFrameTypes(), frame.request().type()) < 0) {
                continue;
            }

            ResponseFrameHolder<?> responseFrameHolder = listener.getListener().handleFrame(frame);
            if (responseFrameHolder != null) {
                ChannelFuture channelFuture = ctx.channel().writeAndFlush(toNetty(responseFrameHolder.response()));
                if (responseFrameHolder.closeAfterSent()) {
                    channelFuture.addListener(ChannelFutureListener.CLOSE);
                    return;
                }

                if (responseFrameHolder.lastHandler()) {
                    return;
                }
            }
        }
    }
}
