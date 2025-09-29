package com.example.chat.server.handler;

import com.example.chat.protocol.MessageWrapper;
import com.example.chat.server.handler.biz.BizHandler;
import com.example.chat.server.handler.biz.MessageHandler;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
public class MessageHandlerRouter {
    // 存储 消息类型 -> 处理器方法 信息的映射
    private static final Map<MessageWrapper.PayloadCase, HandlerInfo> HANDLER_MAP = new HashMap<>();

    // 内部类，用于存储处理实例和方法
    private static class HandlerInfo {
        private final BizHandler handler;
        private final Method method;

        public HandlerInfo(BizHandler handler, Method method) {
            this.handler = handler;
            this.method = method;
        }
    }

    // 在服务器启动时调用
    public static void initialize() {
        log.info("开始扫描并注册消息处理器...");
        Reflections reflections = new Reflections("com.example.chat.server.handler.biz");
        Set<Class<? extends BizHandler>> handlerClasses = reflections.getSubTypesOf(BizHandler.class);

        for (Class<? extends BizHandler> clazz : handlerClasses) {
            try {
                BizHandler handlerInstance = clazz.getDeclaredConstructor().newInstance();
                Method[] methods = clazz.getDeclaredMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(MessageHandler.class)) {
                        MessageHandler annotation = method.getAnnotation(MessageHandler.class);
                        MessageWrapper.PayloadCase messageType = annotation.value();
                        HANDLER_MAP.put(messageType, new HandlerInfo(handlerInstance, method));
                        log.info("已注册处理器：{} -> {}", messageType, clazz.getSimpleName() + "." + method.getName());
                    }
                }
            } catch (Exception e) {
                log.error("注册处理器时发生错误： " + clazz.getName(), e);
            }
        }
        log.info("消息处理器扫描注册完成，共 {} 个", HANDLER_MAP.size());
    }

    // 具体的路由方法，在 Netty Handler 中调用
    public static void route(ChannelHandlerContext ctx, MessageWrapper msg) {
        MessageWrapper.PayloadCase messageType = msg.getPayloadCase();
        HandlerInfo handlerInfo = HANDLER_MAP.get(messageType);

        if (handlerInfo == null) {
            log.warn("收到未知的消息类型，无法路由：{}", messageType);
            return;
        }

        try {
            Object payload = getPayload(msg);

            // 目前暂定所有方法的参数只有 2 个
            handlerInfo.method.invoke(handlerInfo.handler, ctx, payload);
        } catch (Exception e) {
            log.error("路由消息时发生错误： " + messageType, e);
        }
    }

    private static Message getPayload(MessageWrapper msg) {
        try {
            // 1. 获取当前消息设置的是 oneof 中的哪个字段
            MessageWrapper.PayloadCase payloadCase = msg.getPayloadCase();
            if (payloadCase == MessageWrapper.PayloadCase.PAYLOAD_NOT_SET) {
                return null;
            }
            // 2. 获取顶层消息 MessageWrapper 的描述符（FieldDescriptor）
            Descriptors.Descriptor messageDescriptor = msg.getDescriptorForType();

            // 3. 使用当前激活字段的编号，从顶层消息描述符中查找对应的字段描述符
            Descriptors.FieldDescriptor fieldDescriptor = messageDescriptor.findFieldByNumber(payloadCase.getNumber());

            if (fieldDescriptor != null) {
                // 4. 使用 getField 方法，传入字段描述符，直接获取字段的值
                return (Message) msg.getField(fieldDescriptor);
            }
        } catch (Exception e) {
            log.error("通过反射提取 payload 失败", e);
        }
        return null;
    }
}