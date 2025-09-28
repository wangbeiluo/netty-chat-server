package com.example.chat.client.handler;

import com.example.chat.protocol.ChatMessage;
import com.example.chat.protocol.LoginResponse;
import com.example.chat.protocol.MessageWrapper;
import com.example.chat.protocol.ServerNotification;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatClientHandler extends SimpleChannelInboundHandler<MessageWrapper> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageWrapper msg) throws Exception {
        if (msg.hasLoginResponse()){
            handleLoginResponse(msg.getLoginResponse());
        } else if (msg.hasChatMessage()){
            handleChatMessage(msg.getChatMessage());
        } else if (msg.hasServerNotification()){
            handleServerNotification(msg.getServerNotification());
        }
    }

    private void handleLoginResponse(LoginResponse response) {
        log.info("登录结果：{}，消息：{}",response.getSuccess(),response.getMessage());
        if (response.getSuccess()){
            log.info("你的用户ID是：{}，现在可以开始聊天了...", response.getUserId());
        }
    }

    private void handleChatMessage(ChatMessage chatMessage) {
        log.info("[{}]说：{}",chatMessage.getFromUserId(),chatMessage.getContent());
    }

    private void handleServerNotification(ServerNotification notification) {
        log.info("[系统通知]: {}", notification.getMessage());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("连接出现异常", cause);
        ctx.close();
    }
}
