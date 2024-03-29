package org.springframework.context.expression;

import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;
import java.util.Arrays;

public class MethodBasedEvaluationContext extends StandardEvaluationContext {

    private final Method method;

    private final Object[] arguments;

    private final ParameterNameDiscoverer parameterNameDiscoverer;

    private boolean argumentsLoaded = false;

    public MethodBasedEvaluationContext(Object rootObject, Method method, Object[] arguments, ParameterNameDiscoverer parameterNameDiscoverer) {
        super(rootObject);
        this.method = method;
        this.arguments = arguments;
        this.parameterNameDiscoverer = parameterNameDiscoverer;
    }

    @Override
    @Nullable
    public Object lookupVariable(String name) {
        Object variable = super.lookupVariable(name);
        if (variable != null) {
            return variable;
        }
        if (!this.argumentsLoaded) {
            lazyLoadArguments();
            this.argumentsLoaded = true;
            variable = super.lookupVariable(name);
        }
        return variable;
    }

    protected void lazyLoadArguments() {
        // Shortcut if no args need to be loaded
        if (ObjectUtils.isEmpty(this.arguments)) {
            return;
        }
        // Expose indexed variables as well as parameter names (if discoverable)
        String[] paramNames = this.parameterNameDiscoverer.getParameterNames(this.method);
        int paramCount = (paramNames != null ? paramNames.length : this.method.getParameterCount());
        int argsCount = this.arguments.length;
        for (int i = 0; i < paramCount; i++) {
            Object value = null;
            if (argsCount > paramCount && i == paramCount - 1) {
                // Expose remaining arguments as vararg array for last parameter
                value = Arrays.copyOfRange(this.arguments, i, argsCount);
            } else if (argsCount > i) {
                // Actual argument found - otherwise left as null
                value = this.arguments[i];
            }
            setVariable("a" + i, value);
            setVariable("p" + i, value);
            if (paramNames != null && paramNames[i] != null) {
                setVariable(paramNames[i], value);
            }
        }
    }

}
