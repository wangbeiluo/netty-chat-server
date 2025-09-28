package com.example.chat.server.handler;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.example.chat.protocol.ChatMessage;
import com.example.chat.protocol.LoginRequest;
import com.example.chat.protocol.LoginResponse;
import com.example.chat.protocol.MessageWrapper;
import com.example.chat.server.SessionManager;
import com.example.chat.server.db.MessageDao;
import com.example.chat.server.db.UserDao;
import com.example.chat.server.model.Message;
import com.example.chat.server.model.User;
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

        Message messageEntity = Message.builder()
                .fromUserId(fromUserId)
                .content(chatMsgDto.getContent())
                .createdAt(System.currentTimeMillis())
                .build();
//        log.info("收到用户[{}]的消息：{}",fromUserId,chatMsg.getContent());

        // 持久化 Message 实体
        try {
            messageDao.saveMessage(messageEntity);
            log.info("消息已成功存入数据库");
        } catch (SQLException e) {
            log.error("消息存入数据库失败", e);
        }

        //TODO 目前简单实现一个广播，将消息发给所有在线用户，后续根据情况实现单发或者群发
        ChatMessage broadcastMsg = ChatMessage.newBuilder()
                .setFromUserId(fromUserId)
                .setFromUsername("某人") // 实际项目应从Session或者DB获取
                .setContent(chatMsgDto.getContent())
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
