# 介绍
[zzs-rpc-framework](https://github.com/fangtexi/zzs-rpc-framework) 是一款基于 Netty + Zookeeper 实现的 RPC 框架，服务提供端 Server 向注册中心注册服务，服务消费者 Client 通过注册中心拿到服务相关信息，然后再通过网络请求服务提供端 Server。基本架构如下👇<br />![rpc.drawio.png](https://cdn.nlark.com/yuque/0/2023/png/29465341/1691988203118-31b546c5-1de9-4149-8c63-7bb1dfa0d066.png#averageHue=%23f9f8f7&clientId=u1b99b002-d1a2-4&from=ui&id=uef131591&originHeight=367&originWidth=838&originalType=binary&ratio=1.25&rotation=0&showTitle=false&size=167929&status=done&style=none&taskId=u32799a2c-838b-42a1-a723-09aea87dcad&title=)

# 基本情况

- 基于 Netty 实现网络传输
- 实现了 hessian、kyro、protostuff 序列化机制
- 使用 zookeeper 管理服务信息
- Netty 重用 Channel 避免重复连接服务端
- 使用 CompletableFuture 包装接受客户端返回结果
- 增加 Netty 心跳机制 : 保证客户端和服务端的连接不被断掉，避免重连。
- 客户端调用远程服务的时候进行负载均衡。ps: 目前实现了一致性哈希算法
- 集成 Spring 通过注解进行注册服务和消费服务
- 通过 SPI 机制动态的获取序列化方式、压缩方式等
- 自定义客户端与服务端的通信协议
   - 魔数
   - 版本号
   - 消息类型
   - 序列化类型
   - 压缩类型
   - 请求id
   - 正文长度
# 项目模块介绍
![image.png](https://cdn.nlark.com/yuque/0/2023/png/29465341/1691988927047-d7faa591-7775-457a-96cc-fe2e703d5ce8.png#averageHue=%23d3a878&clientId=u1b99b002-d1a2-4&from=paste&height=277&id=u2b1fad2f&originHeight=346&originWidth=440&originalType=binary&ratio=1.25&rotation=0&showTitle=false&size=34734&status=done&style=none&taskId=u29cb422a-92bb-4d46-af40-eb889356d2a&title=&width=352)
# 运行项目
## 安装 zookeeper
下载：
```shell
docker pull zookeeper:3.5.8
```
运行：
```shell
docker run -d --name zookeeper -p 2181:2181 zookeeper:3.5.8
```
## 使用
### 服务提供端 -- example-server
发布服务
```java
@RpcScan(basePackage = {"com.zzs"})
public class NettyServerMain {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(NettyServerMain.class);
        NettyRpcServer nettyRpcServer = (NettyRpcServer) applicationContext.getBean("nettyRpcServer");
        nettyRpcServer.start();
    }
}
```
> 程序启动后，实现了 ImportBeanDefinitionRegistrar 的类就会获取到 @RpcScan 注解的 basePackage 属性值，然后通过自定义的包扫描器去扫描指定包路径下的被 @RpcService 注解修饰的类，加载到 bean 容器中，然后在 bean 初始化之前将服务发布到注册中心

### 服务消费
```java
@RpcScan(basePackage = {"github.javaguide"})
public class NettyClientMain {
    public static void main(String[] args) throws InterruptedException {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(NettyClientMain.class);
        HelloController helloController = (HelloController) applicationContext.getBean("helloController");
        helloController.test();
    }
}
```

