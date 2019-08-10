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
// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: ProtoService.proto

package com.alipay.sofa.rpc.server.bolt.pb;

public interface EchoResponseOrBuilder extends
        // @@protoc_insertion_point(interface_extends:com.alipay.sofa.rpc.server.bolt.pb.EchoResponse)
        com.google.protobuf.MessageOrBuilder {

    /**
     * <code>int32 code = 1;</code>
     */
    int getCode();

    /**
     * <code>string message = 2;</code>
     */
    String getMessage();

    /**
     * <code>string message = 2;</code>
     */
    com.google.protobuf.ByteString getMessageBytes();

}
