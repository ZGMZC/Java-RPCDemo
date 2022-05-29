package com.rpcdemo.client;

import com.rpcdemo.Peer;
import com.rpcdemo.codec.Decoder;
import com.rpcdemo.codec.Encoder;
import com.rpcdemo.codec.JSONDecoder;
import com.rpcdemo.codec.JSONEncoder;
import com.rpcdemp.transport.client.HTTPTransportClient;
import com.rpcdemp.transport.client.TransportClient;
import lombok.Data;

import java.util.Arrays;
import java.util.List;
@Data
public class RpcClientConfig {
    private Class<? extends TransportClient> transportClass=HTTPTransportClient.class;
    private Class<? extends Encoder> encoderClass= JSONEncoder.class;
    private Class<? extends Decoder> decoderClass= JSONDecoder.class;

    private Class<? extends TransportSelector> selectorClass=RandomTransportSelector.class;
    private int connectCount=5;
    private List<Peer> servers= Arrays.asList(
        new Peer("127.0.0.1",3000)
    );
}
