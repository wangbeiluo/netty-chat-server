package com.example.chat.server.handler;

import com.example.chat.protocol.*;
import com.example.chat.server.handler.biz.AuthHandler;
import com.example.chat.server.handler.biz.ChatHandler;
import com.example.chat.server.handler.biz.RoomHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatServerHandler extends SimpleChannelInboundHandler<MessageWrapper> {

    private final AuthHandler authHandler = new AuthHandler();
    private final RoomHandler roomHandler = new RoomHandler();
    private final ChatHandler chatHandler = new ChatHandler();
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageWrapper msg) throws Exception {
        MessageHandlerRouter.route(ctx, msg);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端连接: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端断开: {}", ctx.channel().remoteAddress());
        // TODO: 在此集成 Session 管理
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("发生异常: {}", cause.getMessage());
        ctx.close();
    }
}
