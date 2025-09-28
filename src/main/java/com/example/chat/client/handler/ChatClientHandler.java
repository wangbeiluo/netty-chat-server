package com.example.chat.client.handler;

import com.example.chat.client.ChatClient;
import com.example.chat.protocol.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatClientHandler extends SimpleChannelInboundHandler<MessageWrapper> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageWrapper msg) throws Exception {
        switch (msg.getPayloadCase()) {
            case LOGIN_RESPONSE:
                handleLoginResponse(msg.getLoginResponse());
                break;
            case CHAT_MESSAGE:
                handleChatMessage(msg.getChatMessage());
                break;
            case SERVER_NOTIFICATION:
                handleServerNotification(msg.getServerNotification());
                break;
            case CREATE_ROOM_RESPONSE:
                handleCreateRoomResponse(msg.getCreateRoomResponse());
                break;
            case JOIN_ROOM_RESPONSE:
                handleJoinRoomResponse(msg.getJoinRoomResponse());
                break;
            default:
                log.warn("收到位置的消息类型：{}", msg.getPayloadCase());
        }
    }

    private void handleLoginResponse(LoginResponse response) {
        log.info("登录结果：{}，消息：{}", response.getSuccess(), response.getMessage());
        if (response.getSuccess()) {
            log.info("你的用户ID是：{}，现在可以开始聊天了...", response.getUserId());
        }
    }

    private void handleChatMessage(ChatMessage chatMessage) {
        log.info("[{}]说：{}", chatMessage.getFromUserId(), chatMessage.getContent());
    }

    private void handleServerNotification(ServerNotification notification) {
        log.info("[系统通知]: {}", notification.getMessage());
    }


    private void handleCreateRoomResponse(CreateRoomResponse createRoomResponse) {
        log.info("创建房间结果：{}，消息：{}，房间ID：{}",
                createRoomResponse.getSuccess(), createRoomResponse.getMessage(), createRoomResponse.getRoomId());
    }

    private void handleJoinRoomResponse(JoinRoomResponse joinRoomResponse) {
        log.info("加入房间结果：{}，消息：{}", joinRoomResponse.getSuccess(), joinRoomResponse.getMessage());
        if (joinRoomResponse.getSuccess()) {
            // 如果成功加入房间，则更新客户端的当前房间ID状态
            ChatClient.setCurrentRoomId(joinRoomResponse.getRoomId());
            log.info("你现在位于房间【{}】，可以开始聊天了", joinRoomResponse.getRoomId());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("连接出现异常", cause);
        ctx.close();
    }
}
