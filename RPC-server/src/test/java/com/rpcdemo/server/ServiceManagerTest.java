package com.rpcdemo.server;

import com.rpcdemo.Request;
import com.rpcdemo.ServiceDescriptor;
import com.rpcdemo.common.utils.ReflectionUtils;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class ServiceManagerTest {
    ServiceManager sm;
    @Before
    public void init(){
        sm=new ServiceManager();
        TestInterface bean=new TsetClass();
        sm.register(TestInterface.class,bean);
    }
    @Test
    public void register() {
        TestInterface bean=new TsetClass();
        sm.register(TestInterface.class,bean);
    }

    @Test
    public void lookup() {
        Method method = ReflectionUtils.getPublicMethods(TestInterface.class)[0];
        ServiceDescriptor sdp=ServiceDescriptor.from(TestInterface.class,method);
        Request request=new Request();
        request.setService(sdp);
        ServiceInstance sis = sm.lookup(request);
        System.out.println(sis);
    }
}