package com.example.chat.server;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SessionManager {
    // userId -> Channel
    public static final Map<String, Channel> USER_CHANNEL_MAP = new ConcurrentHashMap<>();
    // Channel -> userId
    public static final Map<Channel, String> CHANNEL_USER_MAP = new ConcurrentHashMap<>();

    public static void bind(String userId, Channel channel) {
        USER_CHANNEL_MAP.put(userId, channel);
        CHANNEL_USER_MAP.put(channel, userId);
        log.info("用户{}绑定会话成功，当前在线人数：{}",userId,USER_CHANNEL_MAP.size());
    }

    public static void unbind(Channel channel) {
        String userId = CHANNEL_USER_MAP.remove(channel);
        if (userId != null) {
            USER_CHANNEL_MAP.remove(userId);
            log.info("用户{}解绑会话成功，当前在线人数：{}",userId,USER_CHANNEL_MAP.size());
        }
    }

    public static Channel getChannel(String userId) {
        return USER_CHANNEL_MAP.get(userId);
    }

    public static String getUserId(Channel channel) {
        return CHANNEL_USER_MAP.get(channel);
    }
}
