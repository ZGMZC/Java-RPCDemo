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
