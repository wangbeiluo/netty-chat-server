package com.example.chat.server.db;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
public class DatabaseUtil {
    private static final String DB_URL = "jdbc:h2:mem:chat_db;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL,DB_USER,DB_PASSWORD);
    }

    public static void initialize() {
        log.info("初始化数据库...");
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()){
            // 创建 users 表
            String createUserTableSql = "CREATE TABLE IF NOT EXISTS users(" +
                                     "id VARCHAR(255) PRIMARY KEY, " +
                                     "username VARCHAR(255) UNIQUE NOT NULL, " +
                                     "password_hash VARCHAR(255) NOT NULL, " +
                                     "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
            stmt.execute(createUserTableSql);
            log.info("用户表 'users' 创建成功或已存在");

            // 创建 message 表
            String createMessageTableSql = "CREATE TABLE IF NOT EXISTS message(" +
                                           "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                                           "from_user_id VARCHAR(255) NOT NULL, " +
                                           "to_user_id VARCHAR(255), " + // 私聊时使用，群聊/广播为NULL
                                           "content TEXT NOT NULL, " +
                                           "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                           "FOREIGN KEY (from_user_id) REFERENCES users(id))";
            stmt.execute(createMessageTableSql);
            log.info("消息表 'message' 创建成功或已存在");
        } catch (SQLException e) {
            log.error("数据库初始化失败",e);
            //TODO 实际项目应抛出异常并终止程序
        }
    }
}
