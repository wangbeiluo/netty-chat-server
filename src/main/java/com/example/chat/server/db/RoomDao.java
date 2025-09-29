package com.example.chat.server.db;

import com.example.chat.protocol.MemberInfo;
import com.example.chat.protocol.RoomInfo;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class RoomDao {

    public String createRoom(String roomName, String creatorId) throws SQLException {
        String roomId = UUID.randomUUID().toString().substring(0, 8);
        String sql = "INSERT INTO rooms (id, name, creator_id) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, roomId);
            pstmt.setString(2, roomName);
            pstmt.setString(3, creatorId);
            pstmt.executeUpdate();
            return roomId;
        }
    }

    public void addMember(String roomId, String userId) throws SQLException {
        String sql = "INSERT INTO room_members (room_id, user_id) VALUES (?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, roomId);
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
        }
    }

    public void delMember(String roomId, String userId) throws SQLException {
        String sql = "DELETE FROM room_members WHERE room_id = ? AND user_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, roomId);
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
        }
    }

    public void delRoom(String roomId) throws SQLException {
        String deleteMemberSql = "DELETE FROM room_members WHERE room_id = ?";
        String deleteRoomSql = "DELETE FROM rooms WHERE room_id = ?";
        try (Connection conn = DatabaseUtil.getConnection()) {
            // 1. 先删除房间内的所有成员
            try (PreparedStatement pstmt = conn.prepareStatement(deleteMemberSql)){
                pstmt.setString(1, roomId);
                pstmt.executeUpdate();
            }
            // 2. 再删除房间
            try (PreparedStatement pstmt = conn.prepareStatement(deleteRoomSql)){
                pstmt.setString(1, roomId);
                pstmt.executeUpdate();
            }
        }
    }

    public List<RoomInfo> getAllRooms() throws SQLException {
        List<RoomInfo> roomList = new ArrayList<>();
        String sql = "SELECT r.id, r.name, COUNT(m.user_id) as member_count " +
                    "FROM rooms r LEFT JOIN room_members m ON r.id = m.room_id " +
                    "GROUP BY r.id, r.name";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                RoomInfo roomInfo = RoomInfo.newBuilder()
                        .setRoomId(rs.getString("id"))
                        .setRoomName(rs.getString("name"))
                        .setMemberCount(rs.getInt("member_count"))
                        .build();
                roomList.add(roomInfo);
            }
        }
        return roomList;
    }

    public List<MemberInfo> getMembersByRoomId(String roomId) throws SQLException {
        List<MemberInfo> memberList = new ArrayList<>();
        String sql = "SELECT u.id, u.username FROM users u " +
                    "JOIN room_members rm ON u.id = rm.user_id " +
                    "WHERE rm.room_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, roomId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    MemberInfo memberInfo = MemberInfo.newBuilder()
                            .setUserId(rs.getString("id"))
                            .setUserName(rs.getString("username"))
                            .build();
                    memberList.add(memberInfo);
                }
            }
        }
        return memberList;
    }
}
