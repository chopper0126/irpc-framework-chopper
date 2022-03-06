package org.idea.irpc.framework.core.dispatcher;

import org.idea.irpc.framework.core.common.RpcInvocation;
import org.idea.irpc.framework.core.common.RpcProtocol;
import org.idea.irpc.framework.core.common.exception.IRpcException;
import org.idea.irpc.framework.core.server.ServerChannelReadData;

import java.lang.reflect.Method;
import java.util.concurrent.*;

import static org.idea.irpc.framework.core.common.cache.CommonServerCache.*;
import static org.idea.irpc.framework.core.common.cache.CommonServerCache.SERVER_SERIALIZE_FACTORY;

/**
 * @Author linhao
 * @Date created in 11:08 上午 2022/2/13
 */
public class ServerChannelDispatcher {

    private BlockingQueue<ServerChannelReadData> RPC_DATA_QUEUE;

    private ExecutorService executorService;

    public ServerChannelDispatcher() {

    }

    public void init(int queueSize, int bizThreadNums) {
        RPC_DATA_QUEUE = new ArrayBlockingQueue<>(queueSize);
        executorService = new ThreadPoolExecutor(bizThreadNums, bizThreadNums,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(512));
    }

    public void add(ServerChannelReadData serverChannelReadData) {
        //这里面加入更细粒度的限流策略
        RPC_DATA_QUEUE.add(serverChannelReadData);
    }

    class ServerJobCoreHandle implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    ServerChannelReadData serverChannelReadData = RPC_DATA_QUEUE.take();
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            RpcProtocol rpcProtocol = serverChannelReadData.getRpcProtocol();
                            RpcInvocation rpcInvocation = SERVER_SERIALIZE_FACTORY.deserialize(rpcProtocol.getContent(), RpcInvocation.class);
                            //执行过滤链路
                            try {
                                SERVER_BEFORE_FILTER_CHAIN.doFilter(rpcInvocation);
                            }catch (Exception cause){
                                //针对自定义异常进行捕获，并且直接返回异常信息给到客户端，然后打印结果
                                if (cause instanceof IRpcException) {
                                    IRpcException rpcException = (IRpcException) cause;
                                    RpcInvocation reqParam = rpcException.getRpcInvocation();
                                    rpcInvocation.setE(rpcException);
                                    byte[] body = SERVER_SERIALIZE_FACTORY.serialize(reqParam);
                                    RpcProtocol respRpcProtocol = new RpcProtocol(body);
                                    serverChannelReadData.getChannelHandlerContext().writeAndFlush(respRpcProtocol);
                                    return;
                                }
                            }
                            Object aimObject = PROVIDER_CLASS_MAP.get(rpcInvocation.getTargetServiceName());
                            Method[] methods = aimObject.getClass().getDeclaredMethods();
                            Object result = null;
                            for (Method method : methods) {
                                if (method.getName().equals(rpcInvocation.getTargetMethod())) {
                                    if (method.getReturnType().equals(Void.TYPE)) {
                                        try {
                                            method.invoke(aimObject, rpcInvocation.getArgs());
                                        } catch (Exception e) {
                                            rpcInvocation.setE(e);
                                        }
                                    } else {
                                        try {
                                            result = method.invoke(aimObject, rpcInvocation.getArgs());
                                        } catch (Exception e) {
                                            rpcInvocation.setE(e);
                                        }
                                    }
                                    break;
                                }
                            }
                            rpcInvocation.setResponse(result);
                            SERVER_AFTER_FILTER_CHAIN.doFilter(rpcInvocation);
                            RpcProtocol respRpcProtocol = new RpcProtocol(SERVER_SERIALIZE_FACTORY.serialize(rpcInvocation));
                            serverChannelReadData.getChannelHandlerContext().writeAndFlush(respRpcProtocol);
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void startDataConsume() {
        Thread thread = new Thread(new ServerJobCoreHandle());
        thread.start();
    }


}
