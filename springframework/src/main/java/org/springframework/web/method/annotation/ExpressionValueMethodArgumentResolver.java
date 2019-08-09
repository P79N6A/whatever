package org.springframework.web.method.annotation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;

import javax.servlet.ServletException;

public class ExpressionValueMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

    public ExpressionValueMethodArgumentResolver(@Nullable ConfigurableBeanFactory beanFactory) {
        super(beanFactory);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(Value.class);
    }

    @Override
    protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
        Value ann = parameter.getParameterAnnotation(Value.class);
        Assert.state(ann != null, "No Value annotation");
        return new ExpressionValueNamedValueInfo(ann);
    }

    @Override
    @Nullable
    protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest webRequest) throws Exception {
        // No name to resolve
        return null;
    }

    @Override
    protected void handleMissingValue(String name, MethodParameter parameter) throws ServletException {
        throw new UnsupportedOperationException("@Value is never required: " + parameter.getMethod());
    }

    private static final class ExpressionValueNamedValueInfo extends NamedValueInfo {

        private ExpressionValueNamedValueInfo(Value annotation) {
            super("@Value", false, annotation.value());
        }

    }

}
