package com.example.chat.client;

import com.example.chat.protocol.*;
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

    private static String currentRoomId = null;

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
        // 1. 发送登录消息
        LoginRequest loginRequest = LoginRequest.newBuilder()
                .setUsername(username)
                .setPassword(password)
                .build();
        MessageWrapper messageWrapper = MessageWrapper.newBuilder().setLoginRequest(loginRequest).build();
        channel.writeAndFlush(messageWrapper);

        // 登录后，启动一个新线程来处理后续的命令和聊天输入
        new Thread(() -> {
            log.info("--- 命令提示 ---");
            log.info("/create <房间名> - 创建房间");
            log.info("/join <房间ID> - 加入房间");
            log.info("直接输入内容 - 在当前房间发送消息");
            log.info("-----------------");

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("/create ")) {
                    String roomName = line.substring(8).trim();
                    CreateRoomRequest createRoomRequest = CreateRoomRequest.newBuilder().setRoomName(roomName).build();
                    channel.writeAndFlush(MessageWrapper.newBuilder().setCreateRoomRequest(createRoomRequest).build());
                } else if (line.startsWith("/join")) {
                    String roomId = line.substring(6).trim();
                    JoinRoomRequest joinRoomRequest = JoinRoomRequest.newBuilder().setRoomId(roomId).build();
                    channel.writeAndFlush(MessageWrapper.newBuilder().setJoinRoomRequest(joinRoomRequest).build());
                } else {
                    // 默认视为聊天消息
                    if (currentRoomId == null) {
                        log.warn("你当前不在任何房间，请先使用 /join <房间ID> 加入一个房间。");
                        continue;
                    }
                    ChatMessage chatMsg = ChatMessage.newBuilder()
                            .setRoomId(currentRoomId)
                            .setContent(line)
                            .build();
                    channel.writeAndFlush(MessageWrapper.newBuilder().setChatMessage(chatMsg).build());

                }

            }
        }).start();
    }

    public static void setCurrentRoomId(String roomId) {
        currentRoomId = roomId;
    }

    public static void main(String[] args) throws Exception {
        new ChatClient("localhost", 8080).start();
    }
}
