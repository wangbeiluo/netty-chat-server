package com.example.chat.server.handler.biz;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.example.chat.protocol.LoginRequest;
import com.example.chat.protocol.LoginResponse;
import com.example.chat.protocol.MessageWrapper;
import com.example.chat.server.SessionManager;
import com.example.chat.server.db.UserDao;
import com.example.chat.server.model.User;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AuthHandler implements BizHandler {
    private final UserDao userDao = new UserDao();

    @MessageHandler(MessageWrapper.PayloadCase.LOGIN_REQUEST)
    public void handleLoginRequest(ChannelHandlerContext ctx, LoginRequest req){
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

}
