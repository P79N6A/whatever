package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.beanutil.JavaBeanAccessor;
import org.apache.dubbo.common.beanutil.JavaBeanDescriptor;
import org.apache.dubbo.common.beanutil.JavaBeanSerializeUtil;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.io.UnsafeByteArrayInputStream;
import org.apache.dubbo.common.io.UnsafeByteArrayOutputStream;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.common.utils.PojoUtils;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.service.GenericException;
import org.apache.dubbo.rpc.service.GenericService;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import java.io.IOException;
import java.lang.reflect.Method;

import static org.apache.dubbo.common.constants.RpcConstants.*;

@Activate(group = CommonConstants.PROVIDER, order = -20000)
public class GenericFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation inv) throws RpcException {
        if (inv.getMethodName().equals($INVOKE) && inv.getArguments() != null && inv.getArguments().length == 3 && !GenericService.class.isAssignableFrom(invoker.getInterface())) {
            String name = ((String) inv.getArguments()[0]).trim();
            String[] types = (String[]) inv.getArguments()[1];
            Object[] args = (Object[]) inv.getArguments()[2];
            try {
                Method method = ReflectUtils.findMethodByMethodSignature(invoker.getInterface(), name, types);
                Class<?>[] params = method.getParameterTypes();
                if (args == null) {
                    args = new Object[params.length];
                }
                String generic = inv.getAttachment(GENERIC_KEY);
                if (StringUtils.isBlank(generic)) {
                    generic = RpcContext.getContext().getAttachment(GENERIC_KEY);
                }
                if (StringUtils.isEmpty(generic) || ProtocolUtils.isDefaultGenericSerialization(generic)) {
                    args = PojoUtils.realize(args, params, method.getGenericParameterTypes());
                } else if (ProtocolUtils.isJavaGenericSerialization(generic)) {
                    for (int i = 0; i < args.length; i++) {
                        if (byte[].class == args[i].getClass()) {
                            try (UnsafeByteArrayInputStream is = new UnsafeByteArrayInputStream((byte[]) args[i])) {
                                args[i] = ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(GENERIC_SERIALIZATION_NATIVE_JAVA).deserialize(null, is).readObject();
                            } catch (Exception e) {
                                throw new RpcException("Deserialize argument [" + (i + 1) + "] failed.", e);
                            }
                        } else {
                            throw new RpcException("Generic serialization [" + GENERIC_SERIALIZATION_NATIVE_JAVA + "] only support message type " + byte[].class + " and your message type is " + args[i].getClass());
                        }
                    }
                } else if (ProtocolUtils.isBeanGenericSerialization(generic)) {
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] instanceof JavaBeanDescriptor) {
                            args[i] = JavaBeanSerializeUtil.deserialize((JavaBeanDescriptor) args[i]);
                        } else {
                            throw new RpcException("Generic serialization [" + GENERIC_SERIALIZATION_BEAN + "] only support message type " + JavaBeanDescriptor.class.getName() + " and your message type is " + args[i].getClass().getName());
                        }
                    }
                } else if (ProtocolUtils.isProtobufGenericSerialization(generic)) {
                    if (args.length == 1 && args[0] instanceof String) {
                        try (UnsafeByteArrayInputStream is = new UnsafeByteArrayInputStream(((String) args[0]).getBytes())) {
                            args[0] = ExtensionLoader.getExtensionLoader(Serialization.class).getExtension("" + GENERIC_SERIALIZATION_PROTOBUF).deserialize(null, is).readObject(method.getParameterTypes()[0]);
                        } catch (Exception e) {
                            throw new RpcException("Deserialize argument failed.", e);
                        }
                    } else {
                        throw new RpcException("Generic serialization [" + GENERIC_SERIALIZATION_PROTOBUF + "] only support one" + String.class.getName() + " argument and your message size is " + args.length + " and type is" + args[0].getClass().getName());
                    }
                }
                Result result = invoker.invoke(new RpcInvocation(method, args, inv.getAttachments()));
                if (result.hasException() && !(result.getException() instanceof GenericException)) {
                    return new RpcResult(new GenericException(result.getException()));
                }
                if (ProtocolUtils.isJavaGenericSerialization(generic)) {
                    try {
                        UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream(512);
                        ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(GENERIC_SERIALIZATION_NATIVE_JAVA).serialize(null, os).writeObject(result.getValue());
                        return new RpcResult(os.toByteArray());
                    } catch (IOException e) {
                        throw new RpcException("Generic serialization [" + GENERIC_SERIALIZATION_NATIVE_JAVA + "] serialize result failed.", e);
                    }
                } else if (ProtocolUtils.isBeanGenericSerialization(generic)) {
                    return new RpcResult(JavaBeanSerializeUtil.serialize(result.getValue(), JavaBeanAccessor.METHOD));
                } else if (ProtocolUtils.isProtobufGenericSerialization(generic)) {
                    try {
                        UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream(512);
                        ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(GENERIC_SERIALIZATION_PROTOBUF).serialize(null, os).writeObject(result.getValue());
                        return new RpcResult(os.toString());
                    } catch (IOException e) {
                        throw new RpcException("Generic serialization [" + GENERIC_SERIALIZATION_PROTOBUF + "] serialize result failed.", e);
                    }
                } else {
                    return new RpcResult(PojoUtils.generalize(result.getValue()));
                }
            } catch (NoSuchMethodException e) {
                throw new RpcException(e.getMessage(), e);
            } catch (ClassNotFoundException e) {
                throw new RpcException(e.getMessage(), e);
            }
        }
        return invoker.invoke(inv);
    }

}
