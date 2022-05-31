## 项目三：基于Java的RPC框架实现

### 项目描述

- 实现轻量级RPC框架，使得客户端可以通过网络从远程服务端程序上请求服务

- 动态代理部分使用JDK动态代理

- 网络传输部分使用Http协议进行传输
- Redis实现注册中心（TODO）

### 项目收获

- Fastjson序列化和反序列化
- JDK动态代理
- Http网络编程（Jetty、URL Connection）
- 反射
- 泛型
- Redis（TODO）

### 项目模块

- 协议模块：描述Server与Client通信的协议
  - ServiceDescriptor     服务的描述信息
  - Request  请求Server的具体服务及所带参数
  - Response  Server响应给Client的信息，返回值等
- Server模块：服务器端
  - ServiceInstance   暴露出服务的具体实现
  - ServiceManager  维护RpcServer暴露出的服务
  - RpcServer
- Client模块：客户端
  - RemoteInvoker  将Client端请求与Server做交互
  - RpcCilent
  - RandomTransportSelector -> TransportSelector   随机分配连接端
- 序列化模块：二进制数据与对象数据的转换
  - JSONDecoder -> Decoder   反序列化，二进制数据转换为对象数据
  - JSONENcoder -> Encoder   序列化，对象数据转换为二进制数据
- 网络模块：负责Server与Client之间的通信
  - HTTPTransportServer -> TransportServer
  - HTTPTransportClient -> TransportCilent

### 项目实践

#### 项目创建

- 创建空的Maven项目RPCDemo，其中再创建6个Maven子项目，RPC-client、RPC-codec、RPC-proto、RPC-server、RPC-transpot、RPC-common（通用工具类）

- 添加依赖

```xml
	<dependencyManagement>
        <dependencies>
             <!-- IO -->
            <dependency>
                <groupId>commons-io</groupId>
                <artifactId>commons-io</artifactId>
                <version>2.5</version>
            </dependency>
             <!-- Jetty -->
            <dependency>
                <groupId>org.eclipse.jetty</groupId>
                <artifactId>jetty-servlet</artifactId>
                <version>9.4.19.v20190610</version>
            </dependency>
             <!-- Fastjson -->
            <dependency>
                <groupId>com.alibaba</groupId>
                <artifactId>fastjson</artifactId>
                <version>1.2.7</version>
            </dependency>
        </dependencies>
    </dependencyManagement>	

	<dependencies>
        <!-- 单元测试 -->
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.12</version>
            <scope>test</scope>
        </dependency>
        <!-- lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.20</version>
        </dependency>
        <!-- 日志 -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>1.7.36</version>
        </dependency>
        <!-- 日志实现 -->
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.2.3</version>
        </dependency>
    </dependencies>
	
	<build>
        <plugins>
            <!--指定版本-->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <!--指定JDK版本-->
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
```

> dependencyManagement:
> 在该元素下声明的依赖不会实际引入到模块中，只有在 dependencies 元素下同样声明了该依赖，才会引入到模块中。
> 该元素能够约束 dependencies 下依赖的使用，即 dependencies 声明的依赖若未指定版本，则使用 dependencyManagement 中指定的版本，否则将覆盖 dependencyManagement 中的版本。

#### 协议模块

##### Peer

```java
package com.rpcdemo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 表示网络传输的一个端点
 */
@Data
@AllArgsConstructor
public class Peer {
    private String host;   
    private int port;
}

```

> @AllArgsConstructor, 创建一个带有所有字段的构造方法

##### ServiceDescriptor

```java
package com.rpcdemo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 表示服务
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceDescriptor {
    private String clazz;   //类名
    private String method;	//方法名
    private String returnType;	//返回类型
    private String[] parameterTypes; //参数类型

    public static ServiceDescriptor from(Class clazz, Method method){
        ServiceDescriptor sdp=new ServiceDescriptor();
        sdp.setClazz(clazz.getName());
        sdp.setMethod(method.getName());
        sdp.setReturnType(method.getReturnType().getName());
        Class[] parameterClasses = method.getParameterTypes();
        String[] parameterTypes=new String[parameterClasses.length];
        for(int i=0;i<parameterClasses.length;i++){
            parameterTypes[i]=parameterClasses[i].getName();
        }
        sdp.setParameterTypes(parameterTypes);
        return sdp;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(this==obj) return true;
        if(obj==null|| getClass()!=obj.getClass()) return false;
        ServiceDescriptor that=(ServiceDescriptor) obj;
        return this.toString().equals(that.toString());
    }

    @Override
    public String toString() {
        return "clazz="+clazz
                +",method="+method
                +",returnType="+returnType
                +",parameterTypes="+Arrays.toString(parameterTypes);
    }
}


```

> @NoArgsConstructor ,创建一个默认无参的构造方法

##### Request

```java
package com.rpcdemo;

import lombok.Data;

/**
 * 表示RPC的一个请求
 */
@Data
public class Request {
    private ServiceDescriptor service;
    private Object[] parameters;
}

```

##### Response

```java
package com.rpcdemo;

import lombok.Data;

/**
 * 表示RPC的返回响应
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

```

#### 通用工具类

##### ReflectionUtils

```java
package com.rpcdemo.common.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * 反射工具类
 */
public class ReflectionUtils {
    /**
     * 根据class创建对象
     * @param clazz  代创建对象的类
     * @param <T> 对象类型
     * @return 创建好的对象
     */
    public static <T> T newInstance (Class<T> clazz){
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 获取某个class的共有方法
     * @param clazz
     * @return
     */
    public static Method[] getPublicMethods(Class clazz){
        Method[] methods=clazz.getDeclaredMethods();
        List<Method> pmethods=new ArrayList<>();
        for(Method m:methods){
            if(Modifier.isPublic(m.getModifiers())){
                pmethods.add(m);
            }
        }
        return pmethods.toArray(new Method[0]);
    }

    /**
     * 调用指定对象的指定方法
     * @param obj   被调用方法的对象
     * @param method
     * @param args
     * @return
     */
    public static Object invoke(Object obj,Method method,Object... args){
        try {
            return method.invoke(obj,args);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

```

#### 序列化模块

##### 序列化

###### Encoder接口

```java
package com.rpcdemo.codec;

public interface Encoder {
    public byte[] encode(Object obj);
}

```

###### 实现类

```java
package com.rpcdemo.codec;

import com.alibaba.fastjson.JSON;

/**
 * 基于JSON的序列化实现
 */
public class JSONEncoder implements Encoder{
    @Override
    public byte[] encode(Object obj) {
        return JSON.toJSONBytes(obj);
    }
}

```

##### 反序列化

###### Decoder接口

```java
package com.rpcdemo.codec;

public interface Decoder {
    <T> T decode(byte[] bytes,Class<T> clazz);
}

```

###### 实现类

```java
package com.rpcdemo.codec;

import com.alibaba.fastjson.JSON;

public class JSONDecoder implements Decoder{
    @Override
    public <T> T decode(byte[] bytes, Class<T> clazz) {
        return JSON.parseObject(bytes,clazz);
    }
}

```

#### 网络模块

##### 引入依赖

```xml
 	<dependencies>
        <dependency>
            <groupId>commons-io</groupId>
            <artifactId>commons-io</artifactId>
        </dependency>
        <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>jetty-servlet</artifactId>
        </dependency>
        <dependency>
            <groupId>org.example</groupId>
            <artifactId>RPC-proto</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
```

##### Client协议

###### TransportClient接口

```java
package com.rpcdemp.transport.client;

import com.rpcdemo.Peer;

import java.io.InputStream;

/**
 * 创建连接
 * 发送数据并等待响应
 * 关闭连接
 */
public interface TransportClient {
    public void connect(Peer peer);
    public InputStream write(InputStream data);
    public void close();
}

```

###### 实现类

```java
package com.rpcdemp.transport.client;

import com.rpcdemo.Peer;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 */
public class HTTPTransportClient implements TransportClient {
    private String url;

    @Override
    public void connect(Peer peer) {
        this.url= "http://" +peer.getHost()+":"+peer.getPort();
    }

    @Override
    public InputStream write(InputStream data) {
        try {
            HttpURLConnection urlConnection =(HttpURLConnection) new URL(url).openConnection();
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setRequestMethod("POST");
            urlConnection.connect();
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

    }
}

```

##### Server协议

RequestHandler接口

```java
package com.rpcdemp.transport.client;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * 处理网络请求的handler
 */
public interface RequestHandler {
    public void onRequest(InputStream rece, OutputStream toResp);
}

```

###### TransportServer接口

```java
package com.rpcdemp.transport.client;

/**
 * 启动，监听端口
 * 接受请求
 * 关闭监听
 */
public interface TransportServer {
    public void init(int port, RequestHandler handler);
    public void start();

    public void stop();
}

```

###### 实现类

```java
package com.rpcdemp.transport.client;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Slf4j
public class HTTPTransportServer implements TransportServer{
    private RequestHandler handler;
    private Server server;

    @Override
    public void init(int port, RequestHandler handler) {
        this.server=new Server(port);
        this.handler=handler;
        //servlet接受请求
        ServletContextHandler ctx=new ServletContextHandler();
        server.setHandler(ctx);

        ServletHolder holder=new ServletHolder(new RequestServlet());
        ctx.addServlet(holder,"/*");

    }

    @Override
    public void start() {
        try {
            server.start();
            server.join();
        } catch (Exception e) {
            log.error(e.getMessage(),e);
        }
    }

    @Override
    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            log.error(e.getMessage(),e);
        }
    }
    class RequestServlet extends HttpServlet{
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            log.info("client connect");
            InputStream inputStream = req.getInputStream();
            OutputStream outputStream = resp.getOutputStream();
            if(handler !=null){
                handler.onRequest(inputStream,outputStream);
            }
        }
    }
}

```



#### Server模块

##### 引入依赖

```xml
	<dependencies>
        <dependency>
            <groupId>org.example</groupId>
            <artifactId>RPC-proto</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>org.example</groupId>
            <artifactId>RPC-transport</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>org.example</groupId>
            <artifactId>RPC-codec</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>org.example</groupId>
            <artifactId>RPC-common</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>commons-io</groupId>
            <artifactId>commons-io</artifactId>
        </dependency>
    </dependencies>
```



##### RpcServerConfig

```java
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
```

##### ServInstance

```java
package com.rpcdemo.server;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.lang.reflect.Method;

/**
 * 表示一个具体服务
 */
@Data
@AllArgsConstructor
public class ServiceInstance {
    private Object target;
    private Method method;

}

```

##### ServiceManager

```java
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

```

##### ServiceInvoker

```java
package com.rpcdemo.server;

import com.rpcdemo.Request;
import com.rpcdemo.common.utils.ReflectionUtils;

/**
 * 调用具体服务
 */
public class ServiceInvoker {
    public Object invoke(ServiceInstance service, Request request){
        return ReflectionUtils.invoke(service.getTarget(),service.getMethod(),request.getParameters());
    }
}

```

RpcServer

```java
package com.rpcdemo.server;

import com.rpcdemo.Request;
import com.rpcdemo.Response;
import com.rpcdemo.codec.Decoder;
import com.rpcdemo.codec.Encoder;
import com.rpcdemo.common.utils.ReflectionUtils;
import com.rpcdemp.transport.client.RequestHandler;
import com.rpcdemp.transport.client.TransportServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Slf4j
public class RpcServer {
    private RPCServerConfig config;
    private TransportServer net;
    private Encoder encoder;
    private Decoder decoder;
    private ServiceManager serviceManager;
    private ServiceInvoker serviceInvoker;

    public RpcServer(RPCServerConfig config) {
        this.config=config;

        this.net= ReflectionUtils.newInstance(config.getTransportClass());
        this.net.init(config.getPort(),this.handler);

        this.encoder=ReflectionUtils.newInstance(config.getEncoderClass());

        this.decoder=ReflectionUtils.newInstance(config.getDecoderClass());

        this.serviceManager=new ServiceManager();
        this.serviceInvoker=new ServiceInvoker();
    }
    public <T> void register(Class<T> interfaceClass,T bean){
        serviceManager.register(interfaceClass,bean);
    }

    public void start(){
        this.net.start();
    }
    public void stop(){
        this.net.stop();
    }

    private RequestHandler handler=new RequestHandler() {
        @Override
        public void onRequest(InputStream rece, OutputStream toResp) {
            Response resp=new Response();
            try {
                byte[] inBytes= IOUtils.readFully(rece,rece.available());
                Request request=decoder.decode(inBytes,Request.class);
                log.info("get request: {}",request);
                ServiceInstance sis=serviceManager.lookup(request);
                Object res = serviceInvoker.invoke(sis, request);
                resp.setData(res);
            } catch (Exception e) {
                log.warn(e.getMessage(),e);
                resp.setCode(1);
                resp.setMessage("RpcServer get error"+e.getClass().getName()
                                +":"+e.getMessage());
            }
            finally {
                try {
                    byte[] outBytes=encoder.encode(resp);
                    toResp.write(outBytes);
                    log.info("response client");
                } catch (IOException e) {
                    log.warn(e.getMessage(),e);
                }
            }
        }
    };
}

```

#### Client模块

##### 引入依赖

```xml
 	<dependencies>
        <dependency>
            <groupId>org.example</groupId>
            <artifactId>RPC-proto</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>org.example</groupId>
            <artifactId>RPC-codec</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>org.example</groupId>
            <artifactId>RPC-common</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>commons-io</groupId>
            <artifactId>commons-io</artifactId>
        </dependency>
        <dependency>
            <groupId>org.example</groupId>
            <artifactId>RPC-transport</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
```



##### TransportSelector接口

```java
package com.rpcdemo.client;

import com.rpcdemo.Peer;
import com.rpcdemp.transport.client.TransportClient;

import java.util.List;

/**
 * 表示选择哪个server去连接
 */
public interface TransportSelector {
    /**
     * 初始化selector
     * @param peers 可以连接的server端点信息
     * @param count client与server建立多少个连接
     * @param clazz client 实现class
     */
    void init(List<Peer> peers,int count,Class<? extends TransportClient> clazz);
    /**
     * 选择一个transport与server做交互
     * @return 网络client
     */
    TransportClient select();

    /**
     * 释放掉使用完的client
     * @param client
     */
    void release(TransportClient client);
    void close();
}

```

##### RandomTransportSelector

```JAVA
package com.rpcdemo.client;

import com.rpcdemo.Peer;
import com.rpcdemo.common.utils.ReflectionUtils;
import com.rpcdemp.transport.client.TransportClient;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
public class RandomTransportSelector implements TransportSelector{
    /**
     * 已经连接好的client
     */
    private List<TransportClient> clients;

    public RandomTransportSelector() {
        this.clients = new ArrayList<>();
    }

    @Override
    public synchronized void init(List<Peer> peers, int count, Class<? extends TransportClient> clazz) {
        count=Math.max(count,1);
        for(Peer peer:peers){
            for(int i=0;i<count;i++){
                TransportClient client= ReflectionUtils.newInstance(clazz);
                client.connect(peer);
                clients.add(client);
            }
            log.info("connect server:{}",peer);
        }
    }

    @Override
    public synchronized TransportClient select() {
        int i=new Random().nextInt(clients.size());
        return clients.remove(i);
    }

    @Override
    public synchronized void release(TransportClient client) {
        clients.add(client);
    }

    @Override
    public synchronized void close() {
        for(TransportClient client:clients)
            client.close();
        clients.clear();
    }
}

```

##### RpcClinentCongfig

```java
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

```

##### RemoteInvoker

```java
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

```

##### RpcClient

```java
package com.rpcdemo.client;

import com.rpcdemo.codec.Decoder;
import com.rpcdemo.codec.Encoder;
import com.rpcdemo.common.utils.ReflectionUtils;
import com.rpcdemp.transport.client.TransportClient;

import java.lang.reflect.Proxy;

public class RpcClient {
    private RpcClientConfig config;
    private Encoder encoder;
    private Decoder decoder;
    private TransportSelector transportSelector;

    public RpcClient() {
        this(new RpcClientConfig());
    }

    public RpcClient(RpcClientConfig config) {
        this.config = config;
        this.encoder= ReflectionUtils.newInstance(this.config.getEncoderClass());
        this.decoder= ReflectionUtils.newInstance(this.config.getDecoderClass());
        this.transportSelector=ReflectionUtils.newInstance(this.config.getSelectorClass());

        this.transportSelector.init(
                this.config.getServers(),
                this.config.getConnectCount(),
                this.config.getTransportClass()
        );
    }
    public<T> T  getProxy(Class<T> clazz){
        return (T) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{clazz},
                new RemoteInvoker(clazz,encoder,decoder,transportSelector)
        );
    }
}

```



### RPC测试案例

##### CalcServiceImpl

```java
package com.rpcdemo.example;

public class CalcServiceImpl implements CalcService{
    @Override
    public int add(int a, int b) {
        return a+b;
    }

    @Override
    public int minus(int a, int b) {
        return a-b;
    }
}

```

##### Server

```java
package com.rpcdemo.example;

import com.rpcdemo.server.RPCServerConfig;
import com.rpcdemo.server.RpcServer;

public class Server {
    public static void main(String[] args) {
        RpcServer server=new RpcServer(new RPCServerConfig());
        server.register(CalcService.class,new CalcServiceImpl());
        server.start();
    }
}

```

Client

```java
package com.rpcdemo.example;

import com.rpcdemo.client.RpcClient;

public class Client {
    public static void main(String[] args) {
        RpcClient client=new RpcClient();
        CalcService service = client.getProxy(CalcService.class);
        int r1=service.add(1,2);
        int r2=service.minus(2,1);

        System.out.println(r1+","+r2);
    }
}

```

### 总结

- 要点一：Jetty嵌入
  - Server  网络监听
  - ServletContextHandler  网络处理
  - ServletHolder  托管Servlet服务
- 要点二：动态代理
  - Proxy.newProxyInstance  创建动态代理对象
  - RemoteInvoker implements InvocationHandler
- 改进方向
  - 安全性
  - 服务端处理能力
  - 注册中心
  - 集成能力