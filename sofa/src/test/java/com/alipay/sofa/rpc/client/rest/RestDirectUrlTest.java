/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.rpc.client.rest;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.common.utils.CommonUtils;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.server.rest.RestService;
import com.alipay.sofa.rpc.server.rest.RestServiceImpl;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import com.alipay.sofa.rpc.test.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Callable;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class RestDirectUrlTest extends ActivelyDestroyTest {

    @Test
    public void testAll() {
        // 只有1个线程 执行
        ServerConfig serverConfig = new ServerConfig().setPort(12300).setProtocol(RpcConstants.PROTOCOL_TYPE_REST).setDaemon(true);
        // 发布一个服务，每个请求要执行1秒
        ProviderConfig<RestService> providerConfig = new ProviderConfig<RestService>().setInterfaceId(RestService.class.getName()).setRef(new RestServiceImpl()).setBootstrap("rest").setApplication(new ApplicationConfig().setAppName("serverApp")).setServer(serverConfig).setRegister(false);
        providerConfig.export();
        final ConsumerConfig<RestService> consumerConfig = new ConsumerConfig<RestService>().setInterfaceId(RestService.class.getName()).setDirectUrl("rest://127.0.0.1:12300").setProtocol(RpcConstants.PROTOCOL_TYPE_REST).setBootstrap("rest").setApplication(new ApplicationConfig().setAppName("clientApp")).setReconnectPeriod(1000);
        RestService restService = consumerConfig.refer();
        Assert.assertEquals(restService.query(11), "hello world !null");
        serverConfig.getServer().stop();
        // 关闭后再调用一个抛异常
        try {
            restService.query(11);
        } catch (Exception e) {
            // 应该抛出异常
            Assert.assertTrue(e instanceof SofaRpcException);
        }
        Assert.assertTrue(TestUtils.delayGet(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return CommonUtils.isEmpty(consumerConfig.getConsumerBootstrap().getCluster().getConnectionHolder().getAvailableConnections());
            }
        }, true, 50, 40));
        serverConfig.getServer().start();
        // 等待客户端重连服务端
        Assert.assertTrue(TestUtils.delayGet(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return CommonUtils.isNotEmpty(consumerConfig.getConsumerBootstrap().getCluster().getConnectionHolder().getAvailableConnections());
            }
        }, true, 50, 60));
        Assert.assertEquals(restService.query(11), "hello world !null");
    }

}
