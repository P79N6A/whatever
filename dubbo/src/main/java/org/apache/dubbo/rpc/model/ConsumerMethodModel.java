package org.apache.dubbo.rpc.model;

import java.lang.reflect.Method;
import java.util.Map;

import static org.apache.dubbo.common.constants.RpcConstants.$INVOKE;

public class ConsumerMethodModel {
    private final Method method;

    private final String[] parameterTypes;

    private final Class<?>[] parameterClasses;

    private final Class<?> returnClass;

    private final String methodName;

    private final boolean generic;

    private final AsyncMethodInfo asyncInfo;

    public ConsumerMethodModel(Method method, Map<String, Object> attributes) {
        this.method = method;
        this.parameterClasses = method.getParameterTypes();
        this.returnClass = method.getReturnType();
        this.parameterTypes = this.createParamSignature(parameterClasses);
        this.methodName = method.getName();
        this.generic = methodName.equals($INVOKE) && parameterTypes != null && parameterTypes.length == 3;
        if (attributes != null) {
            asyncInfo = (AsyncMethodInfo) attributes.get(methodName);
        } else {
            asyncInfo = null;
        }
    }

    public Method getMethod() {
        return method;
    }

    public Class<?> getReturnClass() {
        return returnClass;
    }

    public AsyncMethodInfo getAsyncInfo() {
        return asyncInfo;
    }

    public String getMethodName() {
        return methodName;
    }

    public String[] getParameterTypes() {
        return parameterTypes;
    }

    private String[] createParamSignature(Class<?>[] args) {
        if (args == null || args.length == 0) {
            return new String[]{};
        }
        String[] paramSig = new String[args.length];
        for (int x = 0; x < args.length; x++) {
            paramSig[x] = args[x].getName();
        }
        return paramSig;
    }

    public boolean isGeneric() {
        return generic;
    }

    public Class<?>[] getParameterClasses() {
        return parameterClasses;
    }

    public static class AsyncMethodInfo {

        private Object oninvokeInstance;

        private Method oninvokeMethod;

        private Object onreturnInstance;

        private Method onreturnMethod;

        private Object onthrowInstance;

        private Method onthrowMethod;

        public Object getOninvokeInstance() {
            return oninvokeInstance;
        }

        public void setOninvokeInstance(Object oninvokeInstance) {
            this.oninvokeInstance = oninvokeInstance;
        }

        public Method getOninvokeMethod() {
            return oninvokeMethod;
        }

        public void setOninvokeMethod(Method oninvokeMethod) {
            this.oninvokeMethod = oninvokeMethod;
        }

        public Object getOnreturnInstance() {
            return onreturnInstance;
        }

        public void setOnreturnInstance(Object onreturnInstance) {
            this.onreturnInstance = onreturnInstance;
        }

        public Method getOnreturnMethod() {
            return onreturnMethod;
        }

        public void setOnreturnMethod(Method onreturnMethod) {
            this.onreturnMethod = onreturnMethod;
        }

        public Object getOnthrowInstance() {
            return onthrowInstance;
        }

        public void setOnthrowInstance(Object onthrowInstance) {
            this.onthrowInstance = onthrowInstance;
        }

        public Method getOnthrowMethod() {
            return onthrowMethod;
        }

        public void setOnthrowMethod(Method onthrowMethod) {
            this.onthrowMethod = onthrowMethod;
        }

    }

}
