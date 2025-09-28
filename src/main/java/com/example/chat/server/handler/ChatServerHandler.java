package com.example.chat.server.handler;

import com.example.chat.protocol.ChatMessage;
import com.example.chat.protocol.LoginRequest;
import com.example.chat.protocol.LoginResponse;
import com.example.chat.protocol.MessageWrapper;
import com.example.chat.server.SessionManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class ChatServerHandler extends SimpleChannelInboundHandler<MessageWrapper> {
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageWrapper msg) throws Exception {
        if (msg.hasLoginRequest()){
            handleLoginRequest(ctx,msg.getLoginRequest());
        } else if (msg.hasChatMessage()){
            //聊天消息处理
            handleChatMessage(ctx,msg.getChatMessage());
            log.info("收到聊天消息：{}",msg.getChatMessage().getContent());
        } else {
            log.warn("收到未知类型的消息");
        }
    }

    private void handleLoginRequest(ChannelHandlerContext ctx, LoginRequest req){
        String username = req.getUsername();
        log.info("用户{}请求登录",username);

        // 目前暂时随机生成一个用户ID，实际项目中应查询数据库得到
        String userId = UUID.randomUUID().toString().substring(0, 8);

        // 绑定会话
        SessionManager.bind(userId, ctx.channel());

        LoginResponse response = LoginResponse.newBuilder()
                .setSuccess(true)
                .setMessage("登录成功")
                .setUserId(userId)
                .build();

        MessageWrapper wrapper = MessageWrapper.newBuilder().setLoginResponse(response).build();
        ctx.writeAndFlush(wrapper);
    }

    private void handleChatMessage(ChannelHandlerContext ctx, ChatMessage chatMsg){
        String fromUserId = chatMsg.getFromUserId();
        if (fromUserId == null){
            log.warn("收到未登录用户的消息：{}",ctx.channel().id());
            return;
        }

        log.info("收到用户[{}]的消息：{}",fromUserId,chatMsg.getContent());

        //TODO 目前简单实现一个广播，将消息发给所有在线用户，后续根据情况实现单发或者群发
        ChatMessage broadcastMsg = ChatMessage.newBuilder()
                .setFromUserId(fromUserId)
                .setFromUsername("某人") // 实际项目应从Session或者DB获取
                .setContent(chatMsg.getContent())
                .setTimestamp(System.currentTimeMillis())
                .build();

        MessageWrapper wrapper = MessageWrapper.newBuilder().setChatMessage(broadcastMsg).build();

        for (Channel channel: SessionManager.USER_CHANNEL_MAP.values()) {
            channel.writeAndFlush(wrapper);
        }
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
