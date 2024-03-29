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
package com.alipay.sofa.rpc.codec;

import com.alipay.sofa.rpc.ext.Extensible;

/**
 * Compressor
 *
 * @author <a href=mailto:zhanggeng.zg@antfin.com>GengZhang</a>
 */
@Extensible(coded = true)
public interface Compressor {
    /**
     * 字节数组压缩
     *
     * @param src 未压缩的字节数组
     * @return 压缩后的字节数组
     */
    byte[] compress(byte[] src);

    /**
     * 字节数组解压缩
     *
     * @param src 压缩后的源字节数组
     * @return 解压缩后的字节数组
     */
    byte[] deCompress(byte[] src);

}
