package com.example.chat.server.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
    private Long id;
    private String fromUserId;
    private String toUserId;
    private String content;
    private Long createdAt;
    private String roomId;
}
