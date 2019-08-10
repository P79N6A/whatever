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

/**
 * Protobuf enum {@code com.alipay.sofa.rpc.server.bolt.pb.Group}
 */
public enum Group implements com.google.protobuf.ProtocolMessageEnum {
    /**
     * <code>A = 0;</code>
     */
    A(0),
    /**
     * <code>B = 1;</code>
     */
    B(1), UNRECOGNIZED(-1),
    ;

    /**
     * <code>A = 0;</code>
     */
    public static final int A_VALUE = 0;

    /**
     * <code>B = 1;</code>
     */
    public static final int B_VALUE = 1;

    public final int getNumber() {
        if (this == UNRECOGNIZED) {
            throw new IllegalArgumentException("Can't get the number of an unknown enum value.");
        }
        return value;
    }

    /**
     * @deprecated Use {@link #forNumber(int)} instead.
     */
    @Deprecated
    public static Group valueOf(int value) {
        return forNumber(value);
    }

    public static Group forNumber(int value) {
        switch (value) {
            case 0:
                return A;
            case 1:
                return B;
            default:
                return null;
        }
    }

    public static com.google.protobuf.Internal.EnumLiteMap<Group> internalGetValueMap() {
        return internalValueMap;
    }

    private static final com.google.protobuf.Internal.EnumLiteMap<Group> internalValueMap = new com.google.protobuf.Internal.EnumLiteMap<Group>() {
        public Group findValueByNumber(int number) {
            return Group.forNumber(number);
        }
    };

    public final com.google.protobuf.Descriptors.EnumValueDescriptor getValueDescriptor() {
        return getDescriptor().getValues().get(ordinal());
    }

    public final com.google.protobuf.Descriptors.EnumDescriptor getDescriptorForType() {
        return getDescriptor();
    }

    public static final com.google.protobuf.Descriptors.EnumDescriptor getDescriptor() {
        return com.alipay.sofa.rpc.server.bolt.pb.ProtoServiceModels.getDescriptor().getEnumTypes().get(0);
    }

    private static final Group[] VALUES = values();

    public static Group valueOf(com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
        if (desc.getType() != getDescriptor()) {
            throw new IllegalArgumentException("EnumValueDescriptor is not for this type.");
        }
        if (desc.getIndex() == -1) {
            return UNRECOGNIZED;
        }
        return VALUES[desc.getIndex()];
    }

    private final int value;

    private Group(int value) {
        this.value = value;
    }
    // @@protoc_insertion_point(enum_scope:com.alipay.sofa.rpc.server.bolt.pb.Group)
}
