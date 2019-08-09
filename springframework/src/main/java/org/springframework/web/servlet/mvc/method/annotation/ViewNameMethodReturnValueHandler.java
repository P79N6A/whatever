package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

public class ViewNameMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

    @Nullable
    private String[] redirectPatterns;

    public void setRedirectPatterns(@Nullable String... redirectPatterns) {
        this.redirectPatterns = redirectPatterns;
    }

    @Nullable
    public String[] getRedirectPatterns() {
        return this.redirectPatterns;
    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        Class<?> paramType = returnType.getParameterType();
        return (void.class == paramType || CharSequence.class.isAssignableFrom(paramType));
    }

    @Override
    public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
        if (returnValue instanceof CharSequence) {
            String viewName = returnValue.toString();
            mavContainer.setViewName(viewName);
            if (isRedirectViewName(viewName)) {
                mavContainer.setRedirectModelScenario(true);
            }
        } else if (returnValue != null) {
            // should not happen
            throw new UnsupportedOperationException("Unexpected return type: " + returnType.getParameterType().getName() + " in method: " + returnType.getMethod());
        }
    }

    protected boolean isRedirectViewName(String viewName) {
        return (PatternMatchUtils.simpleMatch(this.redirectPatterns, viewName) || viewName.startsWith("redirect:"));
    }

}
