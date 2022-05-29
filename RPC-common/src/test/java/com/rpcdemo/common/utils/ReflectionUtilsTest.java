package com.rpcdemo.common.utils;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class ReflectionUtilsTest {

    @org.junit.Test
    public void newInstance() {
        TestClass t = ReflectionUtils.newInstance(TestClass.class);
        assertNotNull(t);
    }

    @org.junit.Test
    public void getPublicMethods() {
        Method[] methods = ReflectionUtils.getPublicMethods(TestClass.class);
        assertEquals(1,methods.length);
        String name = methods[0].getName();
        System.out.println(name);
    }

    @org.junit.Test
    public void invoke() {
        Method[] methods = ReflectionUtils.getPublicMethods(TestClass.class);
        Method b=methods[0];
        TestClass t=new TestClass();
        Object o = ReflectionUtils.invoke(t, b);
        System.out.println(o);
    }
}