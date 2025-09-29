package com.example.external.core.micro;

import com.example.external.core.ExternalCoreSetting;

/**
 * 与真实用户连接的服务器
 */
public interface MicroBootstrap {
    /**
     * 启动与真实用户连接的服务器
     */
    void startup();

    /**
     * 设置 ExternalCoreSetting
     *
     * @param setting
     */
    void setExternalCoreSetting(ExternalCoreSetting setting);
}
