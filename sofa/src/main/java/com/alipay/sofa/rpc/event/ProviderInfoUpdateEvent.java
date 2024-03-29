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
package com.alipay.sofa.rpc.event;

import com.alipay.sofa.rpc.client.ProviderGroup;
import com.alipay.sofa.rpc.config.ConsumerConfig;

/**
 * ProviderInfoUpdateEvent
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ProviderInfoUpdateEvent implements Event {
    private final ConsumerConfig consumerConfig;

    private final ProviderGroup oldProviderGroup;

    private final ProviderGroup newProviderGroup;

    public ProviderInfoUpdateEvent(ConsumerConfig consumerConfig, ProviderGroup oldProviderGroup, ProviderGroup newProviderGroup) {
        this.consumerConfig = consumerConfig;
        this.oldProviderGroup = oldProviderGroup;
        this.newProviderGroup = newProviderGroup;
    }

    public ConsumerConfig getConsumerConfig() {
        return consumerConfig;
    }

    public ProviderGroup getOldProviderGroup() {
        return oldProviderGroup;
    }

    public ProviderGroup getNewProviderGroup() {
        return newProviderGroup;
    }

}
