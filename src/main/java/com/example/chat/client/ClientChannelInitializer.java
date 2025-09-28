package com.example.chat.client;

import com.example.chat.client.handler.ChatClientHandler;
import com.example.chat.server.handler.ProtobufToWebSocketEncoder;
import com.example.chat.server.handler.WebSocketToProtobufDecoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

import java.net.URI;

public class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final URI webSocketURI;

    public ClientChannelInitializer(URI webSocketURI) {
        this.webSocketURI = webSocketURI;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        //HTTP编解码器，WebSocket需要
        pipeline.addLast(new HttpClientCodec());
        //HTTP消息聚合器
        pipeline.addLast(new HttpObjectAggregator(65536));
        // WebSocket 客户端协议处理器，处理握手和控制帧
        pipeline.addLast(new WebSocketClientProtocolHandler(
                webSocketURI, WebSocketVersion.V13,null,false,null,65536
        ));
        // 自定义协议编解码器
        pipeline.addLast(new WebSocketToProtobufDecoder());
        pipeline.addLast(new ProtobufToWebSocketEncoder());

        // 核心业务处理器
        pipeline.addLast(new ChatClientHandler());
    }
}
