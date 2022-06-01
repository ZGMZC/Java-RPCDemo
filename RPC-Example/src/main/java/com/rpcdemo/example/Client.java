package com.rpcdemo.example;

import com.rpcdemo.client.RpcClient;

public class Client {
    public static void main(String[] args) {
        RpcClient client=new RpcClient();
        CalcService service = client.getProxy(CalcService.class);
        int r1=service.add(1,2);
        int r2=service.minus(2,1);
        System.out.println(r1+","+r2);
    }
}
