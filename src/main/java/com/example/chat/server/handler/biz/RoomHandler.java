package com.example.chat.server.handler.biz;

import com.example.chat.protocol.*;
import com.example.chat.server.SessionManager;
import com.example.chat.server.db.RoomDao;
import com.example.chat.server.session.Room;
import com.example.chat.server.session.RoomManager;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.Optional;

@Slf4j
public class RoomHandler implements BizHandler {
    private final RoomDao roomDao = new RoomDao();

    @MessageHandler(MessageWrapper.PayloadCase.CREATE_ROOM_REQUEST)
    public void handleCreateRoomRequest(ChannelHandlerContext ctx, CreateRoomRequest req) throws SQLException {
        String userId = SessionManager.getUserId(ctx.channel());
        if (userId == null) return; // 未登录用户不能创建

        String roomName = req.getRoomName();
        String roomId = roomDao.createRoom(roomName, userId);

        RoomManager.createRoom(roomId,roomName);

        CreateRoomResponse response = CreateRoomResponse.newBuilder()
                .setSuccess(true)
                .setMessage("房间 '" + roomName + "' 创建成功！")
                .setRoomId(roomId)
                .build();
        ctx.writeAndFlush(MessageWrapper.newBuilder().setCreateRoomResponse(response).build());
    }

    @MessageHandler(MessageWrapper.PayloadCase.JOIN_ROOM_REQUEST)
    public void handleJoinRoomRequest(ChannelHandlerContext ctx, JoinRoomRequest joinRoomRequest) throws SQLException {
        String userId = SessionManager.getUserId(ctx.channel());
        if (userId == null) return;

        String roomId = joinRoomRequest.getRoomId();
        Optional<Room> roomOpt = RoomManager.getRoom(roomId);

        if (roomOpt.isPresent()){
            Room room = roomOpt.get();
            roomDao.addMember(roomId, userId);
            room.addMember(userId, ctx.channel());

            JoinRoomResponse response = JoinRoomResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("成功加入房间 " + room.getRoomName())
                    .setRoomId(roomId)
                    .build();
            ctx.writeAndFlush(MessageWrapper.newBuilder().setJoinRoomResponse(response).build());
        } else {
            JoinRoomResponse response = JoinRoomResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("房间不存在")
                    .build();
            ctx.writeAndFlush(MessageWrapper.newBuilder().setJoinRoomResponse(response).build());
        }
    }
}
