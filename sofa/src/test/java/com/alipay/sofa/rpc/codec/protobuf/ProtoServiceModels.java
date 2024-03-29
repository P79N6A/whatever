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
// source: com/alipay/test/ProtoService.proto

package com.alipay.sofa.rpc.codec.protobuf;

public final class ProtoServiceModels {
    private ProtoServiceModels() {
    }

    public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {
    }

    public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
        registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
    }

    static final com.google.protobuf.Descriptors.Descriptor internal_static_com_alipay_sofa_rpc_remoting_test_EchoStrReq_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_com_alipay_sofa_rpc_remoting_test_EchoStrReq_fieldAccessorTable;

    static final com.google.protobuf.Descriptors.Descriptor internal_static_com_alipay_sofa_rpc_remoting_test_EchoStrRes_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_com_alipay_sofa_rpc_remoting_test_EchoStrRes_fieldAccessorTable;

    public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
        return descriptor;
    }

    private static com.google.protobuf.Descriptors.FileDescriptor descriptor;

    static {
        String[] descriptorData = {"\n\"com/alipay/test/ProtoService.proto\022!co" + "m.alipay.sofa.rpc.remoting.test\"\027\n\nEchoS" + "trReq\022\t\n\001s\030\001 \001(\t\"\027\n\nEchoStrRes\022\t\n\001s\030\002 \001(" + "\t2y\n\014ProtoService\022i\n\007echoStr\022-.com.alipa" + "y.sofa.rpc.remoting.test.EchoStrReq\032-.co" + "m.alipay.sofa.rpc.remoting.test.EchoStrR" + "es\"\000B\026B\022ProtoServiceModelsP\001b\006proto3"};
        com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner = new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
            public com.google.protobuf.ExtensionRegistry assignDescriptors(com.google.protobuf.Descriptors.FileDescriptor root) {
                descriptor = root;
                return null;
            }
        };
        com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(descriptorData, new com.google.protobuf.Descriptors.FileDescriptor[]{}, assigner);
        internal_static_com_alipay_sofa_rpc_remoting_test_EchoStrReq_descriptor = getDescriptor().getMessageTypes().get(0);
        internal_static_com_alipay_sofa_rpc_remoting_test_EchoStrReq_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_com_alipay_sofa_rpc_remoting_test_EchoStrReq_descriptor, new String[]{"S",});
        internal_static_com_alipay_sofa_rpc_remoting_test_EchoStrRes_descriptor = getDescriptor().getMessageTypes().get(1);
        internal_static_com_alipay_sofa_rpc_remoting_test_EchoStrRes_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_com_alipay_sofa_rpc_remoting_test_EchoStrRes_descriptor, new String[]{"S",});
    }
    // @@protoc_insertion_point(outer_class_scope)
}
