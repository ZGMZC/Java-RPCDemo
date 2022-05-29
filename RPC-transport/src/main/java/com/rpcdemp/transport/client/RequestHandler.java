package com.rpcdemp.transport.client;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * 处理网络请求的handler
 */
public interface RequestHandler {
    public void onRequest(InputStream rece, OutputStream toResp);
}
