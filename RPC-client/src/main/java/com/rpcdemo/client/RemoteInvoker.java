package com.rpcdemo.client;

import com.rpcdemo.Request;
import com.rpcdemo.Response;
import com.rpcdemo.ServiceDescriptor;
import com.rpcdemo.codec.Decoder;
import com.rpcdemo.codec.Encoder;
import com.rpcdemp.transport.client.TransportClient;
import jdk.nashorn.internal.ir.EmptyNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 调用远程服务的代理类
 */
@Slf4j
public class RemoteInvoker implements InvocationHandler {
    private Class clazz;
    private Encoder encoder;
    private Decoder decoder;
    private TransportSelector transportSelector;
   public RemoteInvoker(Class clazz, Encoder encoder, Decoder decoder,TransportSelector selector){
        this.clazz=clazz;
        this.encoder=encoder;
        this.decoder=decoder;
        this.transportSelector=selector;
   }
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Request request=new Request();
        request.setService(ServiceDescriptor.from(clazz,method));
        request.setParameters(args);
        Response resp=invokeRemote(request);
        if(resp==null||resp.getCode()!=0) throw new IllegalStateException("fail to invoker remote: "+resp);

        return resp.getData();
    }

    private Response invokeRemote(Request request) {
       Response resp=null;
        TransportClient client=null;
        try {
            client=transportSelector.select();
            byte[] outBytes=encoder.encode(request);
            InputStream revice = client.write(new ByteArrayInputStream(outBytes));
            byte[] inBytes = IOUtils.readFully(revice, revice.available());
            resp=decoder.decode(inBytes,Response.class);
        } catch (IOException e) {
            log.warn(e.getMessage(),e);
            resp=new Response();
            resp.setCode(1);
            resp.setMessage("RpcClient get error: "+e.getClass()
                    +": "+e.getMessage());
        } finally {
            if(client!=null){
                transportSelector.release(client);
            }
        }
       return resp;
    }
}
