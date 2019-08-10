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

/**
 * Protobuf type {@code com.alipay.sofa.rpc.codec.protobuf.EchoStrReq}
 */
public final class EchoStrReq extends com.google.protobuf.GeneratedMessageV3 implements
        // @@protoc_insertion_point(message_implements:com.alipay.sofa.rpc.codec.protobuf.EchoStrReq)
        EchoStrReqOrBuilder {
    // Use EchoStrReq.newBuilder() to construct.
    private EchoStrReq(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private EchoStrReq() {
        s_ = "";
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
        return com.google.protobuf.UnknownFieldSet.getDefaultInstance();
    }

    private EchoStrReq(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        this();
        int mutable_bitField0_ = 0;
        try {
            boolean done = false;
            while (!done) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        done = true;
                        break;
                    default: {
                        if (!input.skipField(tag)) {
                            done = true;
                        }
                        break;
                    }
                    case 10: {
                        String s = input.readStringRequireUtf8();
                        s_ = s;
                        break;
                    }
                }
            }
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
            throw e.setUnfinishedMessage(this);
        } catch (java.io.IOException e) {
            throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
        } finally {
            makeExtensionsImmutable();
        }
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return com.alipay.sofa.rpc.codec.protobuf.ProtoServiceModels.internal_static_com_alipay_sofa_rpc_remoting_test_EchoStrReq_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
        return com.alipay.sofa.rpc.codec.protobuf.ProtoServiceModels.internal_static_com_alipay_sofa_rpc_remoting_test_EchoStrReq_fieldAccessorTable.ensureFieldAccessorsInitialized(com.alipay.sofa.rpc.codec.protobuf.EchoStrReq.class, com.alipay.sofa.rpc.codec.protobuf.EchoStrReq.Builder.class);
    }

    public static final int S_FIELD_NUMBER = 1;

    private volatile Object s_;

    /**
     * <code>optional string s = 1;</code>
     */
    public String getS() {
        Object ref = s_;
        if (ref instanceof String) {
            return (String) ref;
        } else {
            com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
            String s = bs.toStringUtf8();
            s_ = s;
            return s;
        }
    }

    /**
     * <code>optional string s = 1;</code>
     */
    public com.google.protobuf.ByteString getSBytes() {
        Object ref = s_;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
            s_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    private byte memoizedIsInitialized = -1;

    public final boolean isInitialized() {
        byte isInitialized = memoizedIsInitialized;
        if (isInitialized == 1)
            return true;
        if (isInitialized == 0)
            return false;
        memoizedIsInitialized = 1;
        return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
        if (!getSBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 1, s_);
        }
    }

    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1)
            return size;
        size = 0;
        if (!getSBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, s_);
        }
        memoizedSize = size;
        return size;
    }

    private static final long serialVersionUID = 0L;

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof com.alipay.sofa.rpc.codec.protobuf.EchoStrReq)) {
            return super.equals(obj);
        }
        com.alipay.sofa.rpc.codec.protobuf.EchoStrReq other = (com.alipay.sofa.rpc.codec.protobuf.EchoStrReq) obj;
        boolean result = true;
        result = result && getS().equals(other.getS());
        return result;
    }

    @Override
    public int hashCode() {
        if (memoizedHashCode != 0) {
            return memoizedHashCode;
        }
        int hash = 41;
        hash = (19 * hash) + getDescriptorForType().hashCode();
        hash = (37 * hash) + S_FIELD_NUMBER;
        hash = (53 * hash) + getS().hashCode();
        hash = (29 * hash) + unknownFields.hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static com.alipay.sofa.rpc.codec.protobuf.EchoStrReq parseFrom(com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.alipay.sofa.rpc.codec.protobuf.EchoStrReq parseFrom(com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.alipay.sofa.rpc.codec.protobuf.EchoStrReq parseFrom(byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static com.alipay.sofa.rpc.codec.protobuf.EchoStrReq parseFrom(byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.alipay.sofa.rpc.codec.protobuf.EchoStrReq parseFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.alipay.sofa.rpc.codec.protobuf.EchoStrReq parseFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.alipay.sofa.rpc.codec.protobuf.EchoStrReq parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static com.alipay.sofa.rpc.codec.protobuf.EchoStrReq parseDelimitedFrom(java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.alipay.sofa.rpc.codec.protobuf.EchoStrReq parseFrom(com.google.protobuf.CodedInputStream input) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static com.alipay.sofa.rpc.codec.protobuf.EchoStrReq parseFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public Builder newBuilderForType() {
        return newBuilder();
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(com.alipay.sofa.rpc.codec.protobuf.EchoStrReq prototype) {
        return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public Builder toBuilder() {
        return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
        Builder builder = new Builder(parent);
        return builder;
    }

    /**
     * Protobuf type {@code com.alipay.sofa.rpc.codec.protobuf.EchoStrReq}
     */
    public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
            // @@protoc_insertion_point(builder_implements:com.alipay.sofa.rpc.codec.protobuf.EchoStrReq)
            com.alipay.sofa.rpc.codec.protobuf.EchoStrReqOrBuilder {
        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return com.alipay.sofa.rpc.codec.protobuf.ProtoServiceModels.internal_static_com_alipay_sofa_rpc_remoting_test_EchoStrReq_descriptor;
        }

        protected FieldAccessorTable internalGetFieldAccessorTable() {
            return com.alipay.sofa.rpc.codec.protobuf.ProtoServiceModels.internal_static_com_alipay_sofa_rpc_remoting_test_EchoStrReq_fieldAccessorTable.ensureFieldAccessorsInitialized(com.alipay.sofa.rpc.codec.protobuf.EchoStrReq.class, com.alipay.sofa.rpc.codec.protobuf.EchoStrReq.Builder.class);
        }

        // Construct using com.alipay.sofa.rpc.codec.protobuf.EchoStrReq.newBuilder()
        private Builder() {
            maybeForceBuilderInitialization();
        }

        private Builder(BuilderParent parent) {
            super(parent);
            maybeForceBuilderInitialization();
        }

        private void maybeForceBuilderInitialization() {
            if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
            }
        }

        public Builder clear() {
            super.clear();
            s_ = "";
            return this;
        }

        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return com.alipay.sofa.rpc.codec.protobuf.ProtoServiceModels.internal_static_com_alipay_sofa_rpc_remoting_test_EchoStrReq_descriptor;
        }

        public com.alipay.sofa.rpc.codec.protobuf.EchoStrReq getDefaultInstanceForType() {
            return com.alipay.sofa.rpc.codec.protobuf.EchoStrReq.getDefaultInstance();
        }

        public com.alipay.sofa.rpc.codec.protobuf.EchoStrReq build() {
            com.alipay.sofa.rpc.codec.protobuf.EchoStrReq result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        public com.alipay.sofa.rpc.codec.protobuf.EchoStrReq buildPartial() {
            com.alipay.sofa.rpc.codec.protobuf.EchoStrReq result = new com.alipay.sofa.rpc.codec.protobuf.EchoStrReq(this);
            result.s_ = s_;
            onBuilt();
            return result;
        }

        public Builder clone() {
            return (Builder) super.clone();
        }

        public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
            return (Builder) super.setField(field, value);
        }

        public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
            return (Builder) super.clearField(field);
        }

        public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
            return (Builder) super.clearOneof(oneof);
        }

        public Builder setRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, int index, Object value) {
            return (Builder) super.setRepeatedField(field, index, value);
        }

        public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
            return (Builder) super.addRepeatedField(field, value);
        }

        public Builder mergeFrom(com.google.protobuf.Message other) {
            if (other instanceof com.alipay.sofa.rpc.codec.protobuf.EchoStrReq) {
                return mergeFrom((com.alipay.sofa.rpc.codec.protobuf.EchoStrReq) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(com.alipay.sofa.rpc.codec.protobuf.EchoStrReq other) {
            if (other == com.alipay.sofa.rpc.codec.protobuf.EchoStrReq.getDefaultInstance())
                return this;
            if (!other.getS().isEmpty()) {
                s_ = other.s_;
                onChanged();
            }
            onChanged();
            return this;
        }

        public final boolean isInitialized() {
            return true;
        }

        public Builder mergeFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
            com.alipay.sofa.rpc.codec.protobuf.EchoStrReq parsedMessage = null;
            try {
                parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                parsedMessage = (com.alipay.sofa.rpc.codec.protobuf.EchoStrReq) e.getUnfinishedMessage();
                throw e.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    mergeFrom(parsedMessage);
                }
            }
            return this;
        }

        private Object s_ = "";

        /**
         * <code>optional string s = 1;</code>
         */
        public String getS() {
            Object ref = s_;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                String s = bs.toStringUtf8();
                s_ = s;
                return s;
            } else {
                return (String) ref;
            }
        }

        /**
         * <code>optional string s = 1;</code>
         */
        public com.google.protobuf.ByteString getSBytes() {
            Object ref = s_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
                s_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <code>optional string s = 1;</code>
         */
        public Builder setS(String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            s_ = value;
            onChanged();
            return this;
        }

        /**
         * <code>optional string s = 1;</code>
         */
        public Builder clearS() {
            s_ = getDefaultInstance().getS();
            onChanged();
            return this;
        }

        /**
         * <code>optional string s = 1;</code>
         */
        public Builder setSBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            s_ = value;
            onChanged();
            return this;
        }

        public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
            return this;
        }

        public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
            return this;
        }
        // @@protoc_insertion_point(builder_scope:com.alipay.sofa.rpc.codec.protobuf.EchoStrReq)
    }

    // @@protoc_insertion_point(class_scope:com.alipay.sofa.rpc.codec.protobuf.EchoStrReq)
    private static final com.alipay.sofa.rpc.codec.protobuf.EchoStrReq DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new com.alipay.sofa.rpc.codec.protobuf.EchoStrReq();
    }

    public static com.alipay.sofa.rpc.codec.protobuf.EchoStrReq getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<EchoStrReq> PARSER = new com.google.protobuf.AbstractParser<EchoStrReq>() {
        public EchoStrReq parsePartialFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return new EchoStrReq(input, extensionRegistry);
        }
    };

    public static com.google.protobuf.Parser<EchoStrReq> parser() {
        return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<EchoStrReq> getParserForType() {
        return PARSER;
    }

    public com.alipay.sofa.rpc.codec.protobuf.EchoStrReq getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }

}
