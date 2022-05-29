package com.rpcdemo.codec;

import org.junit.Test;

import static org.junit.Assert.*;

public class JSONDecoderTest {

    @Test
    public void decode() {
        Encoder encoder=new JSONEncoder();
        TestBean bean=new TestBean();
        bean.setName("test");
        bean.setAge(22);
        byte[] bytes = encoder.encode(bean);
        Decoder decoder=new JSONDecoder();
        TestBean testBean = decoder.decode(bytes, TestBean.class);
        System.out.println(testBean);
    }
    @Test
    public void encode(){
        Encoder encoder=new JSONEncoder();
        TestBean bean=new TestBean();
        bean.setName("test");
        bean.setAge(22);
        byte[] bytes = encoder.encode(bean);
        System.out.println(bytes);
    }
}