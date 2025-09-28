package com.example.chat.server.handler;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.example.chat.protocol.*;
import com.example.chat.server.SessionManager;
import com.example.chat.server.db.MessageDao;
import com.example.chat.server.db.UserDao;
import com.example.chat.server.model.Message;
import com.example.chat.server.model.User;
import com.example.chat.server.session.Room;
import com.example.chat.server.session.RoomManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ChatServerHandler extends SimpleChannelInboundHandler<MessageWrapper> {

    private final UserDao userDao = new UserDao();
    private final MessageDao messageDao = new MessageDao();
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageWrapper msg) throws Exception {
        switch (msg.getPayloadCase()){
            case LOGIN_REQUEST:
                handleLoginRequest(ctx, msg.getLoginRequest());
                break;
            case CHAT_MESSAGE:
                handleChatMessage(ctx, msg.getChatMessage());
                log.debug("收到聊天消息：{}",msg.getChatMessage().getContent());
                break;
            case CREATE_ROOM_REQUEST:
                handleCreateRoomRequest(ctx, msg.getCreateRoomRequest());
                break;
            case JOIN_ROOM_REQUEST:
                handleJoinRoomRequest(ctx, msg.getJoinRoomRequest());
                break;
            default:
                log.warn("收到未知类型的消息:{}", msg.getPayloadCase());
        }
    }

    private void handleLoginRequest(ChannelHandlerContext ctx, LoginRequest req){
        String username = req.getUsername();
        String password = req.getPassword();

        try {
            Optional<User> userOpt = userDao.findByUsername(username);

            if (userOpt.isPresent()){
                User user = userOpt.get();
                BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), user.getPasswordHash());
                if (result.verified){
                    // 密码正确，登录成功
                    loginSuccess(ctx, user);
                } else {
                    // 密码错误
                    loginFailure(ctx, "用户名或密码错误");
                }
            } else {
                // 用户不存在，自动注册
                log.info("用户【{}】不存在，将为其自动注册",username);
                User newUser = userDao.register(username, password);

                loginSuccess(ctx, newUser);
            }
        } catch (SQLException e){
            log.error("处理登录请求时发送数据库错误",e);
            loginFailure(ctx, "服务器内部错误");
        }
    }

    private void loginSuccess(ChannelHandlerContext ctx, User user) {
        // 绑定会话
        SessionManager.bind(user.getId(), ctx.channel());

        LoginResponse response = LoginResponse.newBuilder()
                .setSuccess(true)
                .setMessage("登录成功")
                .setUserId(user.getId())
                .build();

        ctx.writeAndFlush(MessageWrapper.newBuilder().setLoginResponse(response).build());
    }

    private void loginFailure(ChannelHandlerContext ctx, String message) {
        LoginResponse response = LoginResponse.newBuilder()
                .setSuccess(false)
                .setMessage(message)
                .build();
        ctx.writeAndFlush(MessageWrapper.newBuilder().setLoginResponse(response).build());
        // 登录失败，短暂延迟后关闭连接
        ctx.executor().schedule(() -> ctx.close(), 1, TimeUnit.SECONDS);
    }


    private void handleChatMessage(ChannelHandlerContext ctx, ChatMessage chatMsgDto){
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

    private void handleCreateRoomRequest(ChannelHandlerContext ctx, CreateRoomRequest req) {
        String userId = SessionManager.getUserId(ctx.channel());
        if (userId == null) return; // 未登录用户不能创建

        String roomName = req.getRoomName();
        String roomId = UUID.randomUUID().toString().substring(0, 8);

        //TODO 在 DAO 中将房间信息存入数据库

        RoomManager.createRoom(roomId,roomName);

        CreateRoomResponse response = CreateRoomResponse.newBuilder()
                .setSuccess(true)
                .setMessage("房间 '" + roomName + "' 创建成功！")
                .setRoomId(roomId)
                .build();
        ctx.writeAndFlush(MessageWrapper.newBuilder().setCreateRoomResponse(response).build());
    }

    private void handleJoinRoomRequest(ChannelHandlerContext ctx, JoinRoomRequest joinRoomRequest) {
        String userId = SessionManager.getUserId(ctx.channel());
        if (userId == null) return;

        String roomId = joinRoomRequest.getRoomId();
        Optional<Room> roomOpt = RoomManager.getRoom(roomId);

        if (roomOpt.isPresent()){
            Room room = roomOpt.get();
            room.addMember(userId, ctx.channel());

            //TODO 在 DAO 中将成员关系存入数据库

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
