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
package com.alipay.sofa.rpc.transport.http;

import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.context.RpcInvokeContext;
import com.alipay.sofa.rpc.core.exception.RpcErrorType;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.invoke.SofaResponseCallback;
import com.alipay.sofa.rpc.core.request.RequestBase;
import com.alipay.sofa.rpc.message.ResponseFuture;
import com.alipay.sofa.rpc.server.bolt.pb.EchoRequest;
import com.alipay.sofa.rpc.server.bolt.pb.EchoResponse;
import com.alipay.sofa.rpc.server.bolt.pb.Group;
import com.alipay.sofa.rpc.server.http.HttpService;
import com.alipay.sofa.rpc.server.http.HttpServiceImpl;
import com.alipay.sofa.rpc.test.ActivelyDestroyTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class Http2ClearTextExceptionTest extends ActivelyDestroyTest {

    @Test
    public void testProtobuf() throws InterruptedException {
        // 只有1个线程 执行
        ServerConfig serverConfig = new ServerConfig().setStopTimeout(60000).setPort(12333).setProtocol(RpcConstants.PROTOCOL_TYPE_H2C).setDaemon(true);
        // 发布一个服务，每个请求要执行1秒
        ProviderConfig<HttpService> providerConfig = new ProviderConfig<HttpService>().setInterfaceId(HttpService.class.getName()).setRef(new HttpServiceImpl()).setServer(serverConfig).setApplication(new ApplicationConfig().setAppName("serverApp")).setRegister(false);
        providerConfig.export();
        {
            ConsumerConfig<HttpService> consumerConfig = new ConsumerConfig<HttpService>().setInterfaceId(HttpService.class.getName()).setSerialization(RpcConstants.SERIALIZE_PROTOBUF).setDirectUrl("h2c://127.0.0.1:12333").setApplication(new ApplicationConfig().setAppName("clientApp")).setTimeout(500).setProtocol(RpcConstants.PROTOCOL_TYPE_H2C);
            HttpService httpService = consumerConfig.refer();
            EchoRequest request = EchoRequest.newBuilder().setGroup(Group.B).setName("xxx").build();
            try {
                EchoResponse response = httpService.echoPb(request);
                Assert.fail();
            } catch (SofaRpcException e) {
                Assert.assertTrue(e.getErrorType() == RpcErrorType.SERVER_BIZ);
            }
        }
        {
            ConsumerConfig<HttpService> consumerConfig2 = new ConsumerConfig<HttpService>().setInterfaceId(HttpService.class.getName()).setSerialization(RpcConstants.SERIALIZE_PROTOBUF).setDirectUrl("h2c://127.0.0.1:12333").setApplication(new ApplicationConfig().setAppName("clientApp")).setTimeout(500).setProtocol(RpcConstants.PROTOCOL_TYPE_H2C).setInvokeType(RpcConstants.INVOKER_TYPE_ONEWAY).setRepeatedReferLimit(-1);
            HttpService httpService2 = consumerConfig2.refer();
            EchoRequest request = EchoRequest.newBuilder().setGroup(Group.B).setName("xxx").build();
            try {
                httpService2.echoPb(request);
                // NOT SUPPORTED NOW, If want support this, need add key to head.
                Assert.fail();
            } catch (Exception e) {
                Assert.assertTrue(e instanceof SofaRpcException);
            }
        }
        {
            ConsumerConfig<HttpService> consumerConfig3 = new ConsumerConfig<HttpService>().setInterfaceId(HttpService.class.getName()).setSerialization(RpcConstants.SERIALIZE_PROTOBUF).setDirectUrl("h2c://127.0.0.1:12333").setApplication(new ApplicationConfig().setAppName("clientApp")).setTimeout(500).setProtocol(RpcConstants.PROTOCOL_TYPE_H2C).setInvokeType(RpcConstants.INVOKER_TYPE_FUTURE).setRepeatedReferLimit(-1);
            HttpService httpService3 = consumerConfig3.refer();
            EchoRequest request = EchoRequest.newBuilder().setGroup(Group.B).setName("xxx").build();
            EchoResponse response = httpService3.echoPb(request);
            Assert.assertNull(response);
            ResponseFuture future = RpcInvokeContext.getContext().getFuture();
            try {
                future.get();
                Assert.fail();
            } catch (ExecutionException e) {
                SofaRpcException re = (SofaRpcException) e.getCause();
                Assert.assertTrue(re.getErrorType() == RpcErrorType.SERVER_BIZ);
            }
        }
        {
            final Object[] result = new Object[1];
            final CountDownLatch latch = new CountDownLatch(1);
            ConsumerConfig<HttpService> consumerConfig4 = new ConsumerConfig<HttpService>().setInterfaceId(HttpService.class.getName()).setSerialization(RpcConstants.SERIALIZE_PROTOBUF).setTimeout(500).setDirectUrl("h2c://127.0.0.1:12333").setApplication(new ApplicationConfig().setAppName("clientApp")).setProtocol(RpcConstants.PROTOCOL_TYPE_H2C).setInvokeType(RpcConstants.INVOKER_TYPE_CALLBACK).setOnReturn(new SofaResponseCallback() {
                @Override
                public void onAppResponse(Object appResponse, String methodName, RequestBase request) {
                    result[0] = appResponse;
                    latch.countDown();
                }

                @Override
                public void onAppException(Throwable throwable, String methodName, RequestBase request) {
                    result[0] = throwable;
                    latch.countDown();
                }

                @Override
                public void onSofaException(SofaRpcException exception, String methodName, RequestBase request) {
                    result[0] = exception;
                    latch.countDown();
                }
            }).setRepeatedReferLimit(-1);
            HttpService httpService4 = consumerConfig4.refer();
            EchoRequest request = EchoRequest.newBuilder().setGroup(Group.B).setName("xxx").build();
            EchoResponse response = httpService4.echoPb(request);
            Assert.assertNull(response);
            latch.await(2000, TimeUnit.MILLISECONDS);
            Throwable e = (Throwable) result[0];
            Assert.assertTrue(e instanceof SofaRpcException);
            Assert.assertTrue(((SofaRpcException) e).getErrorType() == RpcErrorType.SERVER_BIZ);
        }
    }

}
