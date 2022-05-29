package com.rpcdemo.server;

import com.rpcdemo.codec.Decoder;
import com.rpcdemo.codec.Encoder;
import com.rpcdemo.codec.JSONDecoder;
import com.rpcdemo.codec.JSONEncoder;
import com.rpcdemp.transport.client.HTTPTransportServer;
import com.rpcdemp.transport.client.TransportServer;
import lombok.Data;

/**
 * server配置
 */
@Data
public class RPCServerConfig {
    private Class<? extends TransportServer> transportClass= HTTPTransportServer.class;
    private Class<? extends Encoder> encoderClass= JSONEncoder.class;
    private Class<? extends Decoder> decoderClass= JSONDecoder.class;
    private int port=3000;

}
