package com.example.chat.server;

import com.example.chat.server.handler.ChatServerHandler;
import com.example.chat.server.handler.ProtobufToWebSocketEncoder;
import com.example.chat.server.handler.WebSocketToProtobufDecoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        //HTTP编解码器
        pipeline.addLast(new HttpServerCodec());
        //大块数据写入处理器
        pipeline.addLast(new ChunkedWriteHandler());
        //HTTP消息聚合器
        pipeline.addLast(new HttpObjectAggregator(65536));
        //WebSocket协议处理器
        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));

        //自定义协议编解码器
        pipeline.addLast(new WebSocketToProtobufDecoder()); // 入站：解码
        pipeline.addLast(new ProtobufToWebSocketEncoder()); // 出战：编码

        //核心业务处理器
        pipeline.addLast(new ChatServerHandler());
    }
}
