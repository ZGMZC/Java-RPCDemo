package com.rpcdemo.server;

import com.rpcdemo.Request;
import com.rpcdemo.ServiceDescriptor;
import com.rpcdemo.common.utils.ReflectionUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理rpc暴露的服务
 */
@Slf4j
public class ServiceManager {
    private Map<ServiceDescriptor,ServiceInstance> service;
    public ServiceManager(){
        this.service=new ConcurrentHashMap<>();
    }
    public <T> void register(Class<T> interfaceClass,T bean){
        Method[] methods = ReflectionUtils.getPublicMethods(interfaceClass);
        for(Method method:methods){
            ServiceInstance sis=new ServiceInstance(bean,method);
            ServiceDescriptor sdp= ServiceDescriptor.from(interfaceClass,method);
            service.put(sdp,sis);
            log.info("register service {} {}",sdp.getClazz(),sdp.getMethod());

        }
    }
    public ServiceInstance lookup(Request request){
        ServiceDescriptor sdp=request.getService();
        return service.get(sdp);
    }
}
