package com.rpcdemp.transport.client;

import com.rpcdemo.Peer;

import java.io.InputStream;

/**
 * 创建连接
 * 发送数据并等待响应
 * 关闭连接
 */
public interface TransportClient {
    public void connect(Peer peer);
    public InputStream write(InputStream data);
    public void close();
}
