package org.idea.irpc.framework.core.client;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import org.idea.irpc.framework.core.common.ChannelFutureWrapper;
import org.idea.irpc.framework.core.common.utils.CommonUtils;
import org.idea.irpc.framework.core.router.IRouter;
import org.idea.irpc.framework.core.router.RotateRouterImpl;
import org.idea.irpc.framework.core.router.Selector;

import javax.xml.stream.events.DTD;
import java.net.UnknownServiceException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static org.idea.irpc.framework.core.common.cache.CommonClientCache.*;

/**
 * 职责： 当注册中心的节点新增或者移除或者权重变化的时候，这个类主要负责对内存中的url做变更
 *
 * @Author linhao
 * @Date created in 11:18 上午 2021/12/16
 */
public class ConnectionHandler {

    /**
     * 核心的连接处理器
     * 专门用于负责和服务端构建连接通信
     */
    private static Bootstrap bootstrap;


    public static void setBootstrap(Bootstrap bootstrap) {
        ConnectionHandler.bootstrap = bootstrap;
    }

    /**
     * 构建单个连接通道 元操作，既要处理连接，还要统一将连接进行内存存储管理
     *
     * @param providerIp
     * @return
     * @throws InterruptedException
     */
    public static void connect(String providerServiceName, String providerIp) throws InterruptedException {
        if (bootstrap == null) {
            throw new RuntimeException("bootstrap can not be null");
        }
        //格式错误类型的信息
        if(!providerIp.contains(":")){
            return;
        }
        String[] providerAddress = providerIp.split(":");
        String ip = providerAddress[0];
        Integer port = Integer.parseInt(providerAddress[1]);
        //到底这个channelFuture里面是什么
        ChannelFuture channelFuture = bootstrap.connect(ip, port).sync();
        ChannelFutureWrapper channelFutureWrapper = new ChannelFutureWrapper();
        channelFutureWrapper.setChannelFuture(channelFuture);
        channelFutureWrapper.setHost(ip);
        channelFutureWrapper.setPort(port);
        SERVER_ADDRESS.add(providerIp);
        List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(providerServiceName);
        if (CommonUtils.isEmptyList(channelFutureWrappers)) {
            channelFutureWrappers = new ArrayList<>();
        }
        channelFutureWrappers.add(channelFutureWrapper);
        CONNECT_MAP.put(providerServiceName, channelFutureWrappers);
    }

    /**
     * 构建ChannelFuture
     * @param ip
     * @param port
     * @return
     * @throws InterruptedException
     */
    public static ChannelFuture createChannelFuture(String ip,Integer port) throws InterruptedException {
        ChannelFuture channelFuture = bootstrap.connect(ip, port).sync();
        return channelFuture;
    }

    /**
     * 断开连接
     *
     * @param providerServiceName
     * @param providerIp
     */
    public static void disConnect(String providerServiceName, String providerIp) {
        SERVER_ADDRESS.remove(providerIp);
        List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(providerServiceName);
        if (CommonUtils.isNotEmptyList(channelFutureWrappers)) {
            Iterator<ChannelFutureWrapper> iterator = channelFutureWrappers.iterator();
            while (iterator.hasNext()) {
                ChannelFutureWrapper channelFutureWrapper = iterator.next();
                if (providerIp.equals(channelFutureWrapper.getHost() + ":" + channelFutureWrapper.getPort())) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * 默认走随机策略获取ChannelFuture
     *
     * @param providerServiceName
     * @return
     */
    public static ChannelFuture getChannelFuture(String providerServiceName) {
        List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(providerServiceName);
        if (CommonUtils.isEmptyList(channelFutureWrappers)) {
            throw new RuntimeException("no provider exist for " + providerServiceName);
        }
        Selector selector = new Selector();
        selector.setSelectStrategy("rotateRouter");
        selector.setProviderServiceName(providerServiceName);
        //todo 随机获取
        ChannelFuture channelFuture = IROUTER.select(selector).getChannelFuture();
        return channelFuture;
    }


}