package com.example.chat.server;

import com.example.chat.server.db.DatabaseUtil;
import com.example.chat.server.handler.MessageHandlerRouter;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatServer {
    
    private final int port;

    public ChatServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        // 在启动 Netty 服务器之前，先初始化消息路由器
        MessageHandlerRouter.initialize();

        DatabaseUtil.initialize();

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ServerChannelInitializer());

            ChannelFuture future = bootstrap.bind(port).sync();
            log.info("服务器启动成功，监听端口: {}", port);

            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully().sync();
            workerGroup.shutdownGracefully().sync();
            log.info("服务器已关闭");
        }
    }

    public static void main(String[] args) throws Exception {
        new ChatServer(8080).start();
    }
}
