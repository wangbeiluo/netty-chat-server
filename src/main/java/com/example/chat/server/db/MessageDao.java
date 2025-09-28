package com.example.chat.server.db;


import com.example.chat.server.model.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MessageDao {
    /**
     * 保存一条聊天消息到数据库
     * @param message
     */
    public void saveMessage(Message message) throws SQLException {
        //TODO 暂未实现私聊功能，to_user_id 暂存为 null
        String sql = "INSERT INTO message(from_user_id, room_id, content, created_at) VALUES (?, ?, ?, ?)";

        try(Connection conn = DatabaseUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, message.getFromUserId());
            pstmt.setString(2, message.getRoomId());
            pstmt.setString(3, message.getContent());
            pstmt.setTimestamp(4, new java.sql.Timestamp(message.getCreatedAt()));
            pstmt.executeUpdate();
        }
    }
}
