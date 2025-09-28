package com.example.chat.server.db;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.example.chat.server.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class UserDao {

    // 注册用户
    public User register(String username, String password) throws SQLException {
        String userId = UUID.randomUUID().toString();
        String passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray());

        String sql = "insert into users (id, username, password_hash) values (?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, userId);
            pstmt.setString(2, username);
            pstmt.setString(3, passwordHash);
            pstmt.executeUpdate();
            return new User(userId, username);
        }
    }

    // 根据用户名查找用户
    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "select id, username, password_hash from users where username = ?";
        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if(rs.next()){
                return Optional.of(new User(
                        rs.getString("id"),
                        rs.getString("username"),
                        rs.getString("password_hash")
                ));
            }
        }
        return Optional.empty();
    }
}
