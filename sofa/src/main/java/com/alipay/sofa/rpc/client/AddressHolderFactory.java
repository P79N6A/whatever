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
package com.alipay.sofa.rpc.client;

import com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap;
import com.alipay.sofa.rpc.common.utils.ExceptionUtils;
import com.alipay.sofa.rpc.core.exception.SofaRpcRuntimeException;
import com.alipay.sofa.rpc.ext.ExtensionClass;
import com.alipay.sofa.rpc.ext.ExtensionLoaderFactory;

/**
 * Factory of address holder.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class AddressHolderFactory {

    /**
     * 根据配置得到连接管理器
     *
     * @param consumerBootstrap 服务消费者配置
     * @return AddressHolder
     */
    public static AddressHolder getAddressHolder(ConsumerBootstrap consumerBootstrap) {
        try {
            String connectionHolder = consumerBootstrap.getConsumerConfig().getAddressHolder();
            ExtensionClass<AddressHolder> ext = ExtensionLoaderFactory.getExtensionLoader(AddressHolder.class).getExtensionClass(connectionHolder);
            if (ext == null) {
                throw ExceptionUtils.buildRuntime("consumer.addressHolder", connectionHolder, "Unsupported addressHolder of client!");
            }
            return ext.getExtInstance(new Class[]{ConsumerBootstrap.class}, new Object[]{consumerBootstrap});
        } catch (SofaRpcRuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new SofaRpcRuntimeException(e.getMessage(), e);
        }
    }

}
