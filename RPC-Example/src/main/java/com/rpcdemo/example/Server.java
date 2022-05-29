package com.rpcdemo.example;

import com.rpcdemo.server.RPCServerConfig;
import com.rpcdemo.server.RpcServer;

public class Server {
    public static void main(String[] args) {
        RpcServer server=new RpcServer(new RPCServerConfig());
        server.register(CalcService.class,new CalcServiceImpl());
        server.start();
    }
}
