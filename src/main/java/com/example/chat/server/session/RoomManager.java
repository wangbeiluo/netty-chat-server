package com.example.chat.server.session;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {
    // roomId -> Room
    private static final Map<String, Room> rooms = new ConcurrentHashMap<>();

    public static void createRoom(String roomId, String roomName) {
        Room room = new Room(roomId, roomName);
        rooms.put(roomId, room);
    }

    public static Optional<Room> getRoom(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    //TODO 添加销毁房间的逻辑
}
