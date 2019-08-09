package org.springframework.aop.aspectj;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.reflect.SourceLocation;
import org.aspectj.runtime.internal.AroundClosure;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class MethodInvocationProceedingJoinPoint implements ProceedingJoinPoint, JoinPoint.StaticPart {

    private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private final ProxyMethodInvocation methodInvocation;

    @Nullable
    private Object[] args;

    @Nullable
    private Signature signature;

    @Nullable
    private SourceLocation sourceLocation;

    public MethodInvocationProceedingJoinPoint(ProxyMethodInvocation methodInvocation) {
        Assert.notNull(methodInvocation, "MethodInvocation must not be null");
        this.methodInvocation = methodInvocation;
    }

    @Override
    public void set$AroundClosure(AroundClosure aroundClosure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object proceed() throws Throwable {
        return this.methodInvocation.invocableClone().proceed();
    }

    @Override
    public Object proceed(Object[] arguments) throws Throwable {
        Assert.notNull(arguments, "Argument array passed to proceed cannot be null");
        if (arguments.length != this.methodInvocation.getArguments().length) {
            throw new IllegalArgumentException("Expecting " + this.methodInvocation.getArguments().length + " arguments to proceed, " + "but was passed " + arguments.length + " arguments");
        }
        this.methodInvocation.setArguments(arguments);
        return this.methodInvocation.invocableClone(arguments).proceed();
    }

    @Override
    public Object getThis() {
        return this.methodInvocation.getProxy();
    }

    @Override
    @Nullable
    public Object getTarget() {
        return this.methodInvocation.getThis();
    }

    @Override
    public Object[] getArgs() {
        if (this.args == null) {
            this.args = this.methodInvocation.getArguments().clone();
        }
        return this.args;
    }

    @Override
    public Signature getSignature() {
        if (this.signature == null) {
            this.signature = new MethodSignatureImpl();
        }
        return this.signature;
    }

    @Override
    public SourceLocation getSourceLocation() {
        if (this.sourceLocation == null) {
            this.sourceLocation = new SourceLocationImpl();
        }
        return this.sourceLocation;
    }

    @Override
    public String getKind() {
        return ProceedingJoinPoint.METHOD_EXECUTION;
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public JoinPoint.StaticPart getStaticPart() {
        return this;
    }

    @Override
    public String toShortString() {
        return "execution(" + getSignature().toShortString() + ")";
    }

    @Override
    public String toLongString() {
        return "execution(" + getSignature().toLongString() + ")";
    }

    @Override
    public String toString() {
        return "execution(" + getSignature().toString() + ")";
    }

    private class MethodSignatureImpl implements MethodSignature {

        @Nullable
        private volatile String[] parameterNames;

        @Override
        public String getName() {
            return methodInvocation.getMethod().getName();
        }

        @Override
        public int getModifiers() {
            return methodInvocation.getMethod().getModifiers();
        }

        @Override
        public Class<?> getDeclaringType() {
            return methodInvocation.getMethod().getDeclaringClass();
        }

        @Override
        public String getDeclaringTypeName() {
            return methodInvocation.getMethod().getDeclaringClass().getName();
        }

        @Override
        public Class<?> getReturnType() {
            return methodInvocation.getMethod().getReturnType();
        }

        @Override
        public Method getMethod() {
            return methodInvocation.getMethod();
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return methodInvocation.getMethod().getParameterTypes();
        }

        @Override
        @Nullable
        public String[] getParameterNames() {
            if (this.parameterNames == null) {
                this.parameterNames = parameterNameDiscoverer.getParameterNames(getMethod());
            }
            return this.parameterNames;
        }

        @Override
        public Class<?>[] getExceptionTypes() {
            return methodInvocation.getMethod().getExceptionTypes();
        }

        @Override
        public String toShortString() {
            return toString(false, false, false, false);
        }

        @Override
        public String toLongString() {
            return toString(true, true, true, true);
        }

        @Override
        public String toString() {
            return toString(false, true, false, true);
        }

        private String toString(boolean includeModifier, boolean includeReturnTypeAndArgs, boolean useLongReturnAndArgumentTypeName, boolean useLongTypeName) {
            StringBuilder sb = new StringBuilder();
            if (includeModifier) {
                sb.append(Modifier.toString(getModifiers()));
                sb.append(" ");
            }
            if (includeReturnTypeAndArgs) {
                appendType(sb, getReturnType(), useLongReturnAndArgumentTypeName);
                sb.append(" ");
            }
            appendType(sb, getDeclaringType(), useLongTypeName);
            sb.append(".");
            sb.append(getMethod().getName());
            sb.append("(");
            Class<?>[] parametersTypes = getParameterTypes();
            appendTypes(sb, parametersTypes, includeReturnTypeAndArgs, useLongReturnAndArgumentTypeName);
            sb.append(")");
            return sb.toString();
        }

        private void appendTypes(StringBuilder sb, Class<?>[] types, boolean includeArgs, boolean useLongReturnAndArgumentTypeName) {
            if (includeArgs) {
                for (int size = types.length, i = 0; i < size; i++) {
                    appendType(sb, types[i], useLongReturnAndArgumentTypeName);
                    if (i < size - 1) {
                        sb.append(",");
                    }
                }
            } else {
                if (types.length != 0) {
                    sb.append("..");
                }
            }
        }

        private void appendType(StringBuilder sb, Class<?> type, boolean useLongTypeName) {
            if (type.isArray()) {
                appendType(sb, type.getComponentType(), useLongTypeName);
                sb.append("[]");
            } else {
                sb.append(useLongTypeName ? type.getName() : type.getSimpleName());
            }
        }

    }

    private class SourceLocationImpl implements SourceLocation {

        @Override
        public Class<?> getWithinType() {
            if (methodInvocation.getThis() == null) {
                throw new UnsupportedOperationException("No source location joinpoint available: target is null");
            }
            return methodInvocation.getThis().getClass();
        }

        @Override
        public String getFileName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getLine() {
            throw new UnsupportedOperationException();
        }

        @Override
        @Deprecated
        public int getColumn() {
            throw new UnsupportedOperationException();
        }

    }

}
