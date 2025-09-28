package com.example.chat.server.handler;

import com.example.chat.protocol.LoginRequest;
import com.example.chat.protocol.LoginResponse;
import com.example.chat.protocol.MessageWrapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ChatServerHandler extends SimpleChannelInboundHandler<MessageWrapper> {

    private static final Logger logger = LoggerFactory.getLogger(ChatServerHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageWrapper msg) throws Exception {
        if (msg.hasLoginRequest()){
            handleLoginRequest(ctx,msg.getLoginRequest());
        } else if (msg.hasChatMessage()){
            //TODO: 聊天消息处理
            logger.info("收到聊天消息：{}",msg.getChatMessage().getContent());
        } else {
            logger.warn("收到未知类型的消息");
        }
    }

    private void handleLoginRequest(ChannelHandlerContext ctx, LoginRequest req){
        logger.info("用户{}请求登录",req.getUsername());
        String userId = UUID.randomUUID().toString().substring(0, 8);

        //TODO: 编写 session 管理

        LoginResponse response = LoginResponse.newBuilder()
                .setSuccess(true)
                .setMessage("登录成功")
                .setUserId(userId)
                .build();

        MessageWrapper wrapper = MessageWrapper.newBuilder().setLoginResponse(response).build();
        ctx.writeAndFlush(wrapper);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        logger.info("客户端连接: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        logger.info("客户端断开: {}", ctx.channel().remoteAddress());
        // TODO: 在此集成 Session 管理
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("发生异常: {}", cause.getMessage());
        ctx.close();
    }
}
