package com.example.chat.server.handler;

import com.example.chat.protocol.MessageWrapper;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.List;

/**
 * 将 Protobuf 对象编码成二进制帧
 */
public class ProtobufToWebSocketEncoder extends MessageToMessageEncoder<MessageWrapper> {

    @Override
    protected void encode(ChannelHandlerContext ctx, MessageWrapper msg, List<Object> out) throws Exception {
        byte[] bytes = msg.toByteArray();
        // 将字节数组包装成 BinaryWebSocketFrame，传递给下一个 Handler
        out.add(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(bytes)));
    }
}
