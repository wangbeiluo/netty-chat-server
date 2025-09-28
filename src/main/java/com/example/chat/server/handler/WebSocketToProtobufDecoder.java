package com.example.chat.server.handler;

import com.example.chat.protocol.MessageWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.List;

/**
 * 将二进制帧解码成 Protobuf 对象
 */
public class WebSocketToProtobufDecoder extends MessageToMessageDecoder<BinaryWebSocketFrame> {

    @Override
    protected void decode(ChannelHandlerContext ctx, BinaryWebSocketFrame msg, List<Object> out) throws Exception {
        ByteBuf buf = msg.content();
        final byte[] array = new byte[buf.readableBytes()];
        buf.readBytes(array);
        // 将字节数组解码成 MessageWrapper 对象，并传递给下一个 Handler
        out.add(MessageWrapper.parseFrom(array));
    }
}
