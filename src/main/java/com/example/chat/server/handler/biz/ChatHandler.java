package com.example.chat.server.handler.biz;

import com.example.chat.protocol.ChatMessage;
import com.example.chat.protocol.MessageWrapper;
import com.example.chat.server.SessionManager;
import com.example.chat.server.db.MessageDao;
import com.example.chat.server.model.Message;
import com.example.chat.server.session.Room;
import com.example.chat.server.session.RoomManager;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.Optional;

@Slf4j
public class ChatHandler implements BizHandler {
    private final MessageDao messageDao = new MessageDao();

    @MessageHandler(MessageWrapper.PayloadCase.CHAT_MESSAGE)
    public void handleChatMessage(ChannelHandlerContext ctx, ChatMessage chatMsgDto){
        String fromUserId = SessionManager.getUserId(ctx.channel());
        if (fromUserId == null){
            log.warn("收到未登录用户的消息：{}",ctx.channel().id());
            return;
        }

        String roomId = chatMsgDto.getRoomId();
        if (roomId == null || roomId.isEmpty()){
            log.warn("收到没有指定 roomId 的聊天信息");
            return;
        }

        Optional<Room> roomOpt = RoomManager.getRoom(roomId);
        if (roomOpt.isPresent()){
            Room room = roomOpt.get();

            ChatMessage broadcastMsg = ChatMessage.newBuilder()
                    .setFromUserId(fromUserId)
                    .setFromUsername("某人") // 实际项目应从Session或者DB获取
                    .setContent(chatMsgDto.getContent())
                    .setTimestamp(System.currentTimeMillis())
                    .build();

            Message messageEntity = Message.builder()
                    .fromUserId(fromUserId)
                    .roomId(roomId)
                    .content(chatMsgDto.getContent())
                    .createdAt(System.currentTimeMillis())
                    .build();

            // 持久化 Message 实体
            try {
                messageDao.saveMessage(messageEntity);
                log.debug("消息已成功存入数据库");
            } catch (SQLException e) {
                log.error("消息存入数据库失败", e);
            }

            room.broadcast(MessageWrapper.newBuilder().setChatMessage(broadcastMsg).build());
        } else {
            log.warn("用户【{}】试图向一个不存在的房间【{}】发送消息",fromUserId,roomId);
        }
    }
}
