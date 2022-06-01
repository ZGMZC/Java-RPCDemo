package com.rpcdemp.transport.client;

import com.rpcdemo.Peer;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * 创建连接
 * 发送数据并等待响应
 * 关闭连接
 */
public class HTTPTransportClient implements TransportClient {
    private String url;
    private HttpURLConnection urlConnection=null;
    @Override
    public void connect(Peer peer) {
        this.url= "http://" +peer.getHost()+":"+peer.getPort();
        try {
            urlConnection =(HttpURLConnection) new URL(url).openConnection();
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setRequestMethod("POST");
            urlConnection.connect();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public InputStream write(InputStream data) {
        try {
            IOUtils.copy(data,urlConnection.getOutputStream());
            int resultCode=urlConnection.getResponseCode();
            if(resultCode==HttpURLConnection.HTTP_OK){
                return urlConnection.getInputStream();
            }else{
                return urlConnection.getErrorStream();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    @Override
    public void close() {
        urlConnection.disconnect();
    }
}
