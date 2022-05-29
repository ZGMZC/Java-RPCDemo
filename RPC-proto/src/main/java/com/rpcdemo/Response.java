package com.rpcdemo;

import lombok.Data;

/**
 * 表示RPC的返回
 */
@Data
public class Response {
    /*服务返回编码，0-成功，非0失败*/
    private int code=0;
    /*具体的错误信息*/
    private String message="OK";
    /*返回的数据*/
    private Object data;
}
