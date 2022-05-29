package com.rpcdemo.client;

import com.rpcdemo.codec.Decoder;
import com.rpcdemo.codec.Encoder;
import com.rpcdemo.common.utils.ReflectionUtils;
import com.rpcdemp.transport.client.TransportClient;

import java.lang.reflect.Proxy;

public class RpcClient {
    private RpcClientConfig config;
    private Encoder encoder;
    private Decoder decoder;
    private TransportSelector transportSelector;

    public RpcClient() {
        this(new RpcClientConfig());
    }

    public RpcClient(RpcClientConfig config) {
        this.config = config;
        this.encoder= ReflectionUtils.newInstance(this.config.getEncoderClass());
        this.decoder= ReflectionUtils.newInstance(this.config.getDecoderClass());
        this.transportSelector=ReflectionUtils.newInstance(this.config.getSelectorClass());

        this.transportSelector.init(
                this.config.getServers(),
                this.config.getConnectCount(),
                this.config.getTransportClass()
        );
    }
    public<T> T  getProxy(Class<T> clazz){
        return (T) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{clazz},
                new RemoteInvoker(clazz,encoder,decoder,transportSelector)
        );
    }
}
