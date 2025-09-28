package com.example.chat.client;

import com.example.chat.protocol.ChatMessage;
import com.example.chat.protocol.LoginRequest;
import com.example.chat.protocol.MessageWrapper;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Scanner;

@Slf4j
public class ChatClient {
    private final String host;
    private final int port;

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            URI webSocketURI = new URI("ws://" + host + ":" + port + "/ws");

            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ClientChannelInitializer(webSocketURI));

            Channel channel = bootstrap.connect(host, port).sync().channel();
            log.info("已成功连接到服务器：{}", webSocketURI);

            // 启动一个新的线程来处理控制台输入
            startConsoleInput(channel);

            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }

    private void startConsoleInput(Channel channel) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("请输入你的用户名:");
        String username = scanner.nextLine();
        System.out.print("请输入你的密码:");
        String password = scanner.nextLine();


        LoginRequest loginRequest = LoginRequest.newBuilder().setUsername(username).setPassword(password).build();
        MessageWrapper messageWrapper = MessageWrapper.newBuilder().setLoginRequest(loginRequest).build();
        channel.writeAndFlush(messageWrapper);

        // 登录后，进入聊天循环
        new Thread(() -> {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.isEmpty()) continue;

                ChatMessage chatMsg = ChatMessage.newBuilder().setContent(line).build();
                channel.writeAndFlush(MessageWrapper.newBuilder().setChatMessage(chatMsg).build());
            }
        }).start();
    }

    public static void main(String[] args) throws Exception {
        new ChatClient("localhost", 8080).start();
    }
}
