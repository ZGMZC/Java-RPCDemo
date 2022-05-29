package com.rpcdemp.transport.client;

/**
 * 启动，监听端口
 * 接受请求
 * 关闭监听
 */
public interface TransportServer {
    public void init(int port, RequestHandler handler);
    public void start();

    public void stop();
}
