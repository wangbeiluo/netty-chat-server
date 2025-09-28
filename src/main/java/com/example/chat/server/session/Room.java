package com.example.chat.server.session;

import com.example.chat.protocol.MessageWrapper;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Getter
public class Room {
    private final String roomId;
    private final String roomName;
    private final Map<String, Channel> members = new ConcurrentHashMap<>();

    public Room(String roomId, String roomName) {
        this.roomId = roomId;
        this.roomName = roomName;
    }

    public void addMember(String userId, Channel channel) {
        members.put(userId, channel);
        log.info("用户【{}】加入房间【{}】，当前房间人数：{}",userId,roomId,roomName);
    }

    public void removeMember(String userId) {
        members.remove(userId);
        log.info("用户【{}】离开房间【{}】，当前房间人数：{}",userId,roomId,roomName);
    }

    public void broadcast(MessageWrapper message) {
        for (Channel channel : members.values()) {
            channel.writeAndFlush(message);
        }
    }
}
