package com.example.external.core;

import com.example.external.core.micro.MicroBootstrap;

/**
 * 它的职责是创建出具体的服务器实例，也是屏蔽底层通信框架细节的抽象工厂
 * - ExternalCore 帮助开发者屏蔽各通信框架的细节，如 Netty、mina、smart-socket 等通信框。
 * - 当前默认提供 Netty 的实现。
 */
public interface ExternalCore {
    /**
     * 创建与真实用户通信的 netty 服务器
     * @return
     */
    MicroBootstrap createMicroBootstrap();
}
