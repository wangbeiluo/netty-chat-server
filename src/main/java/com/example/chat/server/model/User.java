package com.example.chat.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String username;
    private String passwordHash;

    public User(String id, String username) {
        this.id = id;
        this.username = username;
    }
}
