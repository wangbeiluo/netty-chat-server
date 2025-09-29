package com.example.chat.server.handler.biz;

import com.example.chat.protocol.MessageWrapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageHandler {
    // 指定这个方法处理哪种消息类型
    MessageWrapper.PayloadCase value();
}
