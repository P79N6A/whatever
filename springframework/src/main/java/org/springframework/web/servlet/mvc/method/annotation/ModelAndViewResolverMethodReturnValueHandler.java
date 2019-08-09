package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.ModelAttributeMethodProcessor;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.annotation.ModelAndViewResolver;

import java.lang.reflect.Method;
import java.util.List;

public class ModelAndViewResolverMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

    @Nullable
    private final List<ModelAndViewResolver> mavResolvers;

    private final ModelAttributeMethodProcessor modelAttributeProcessor = new ModelAttributeMethodProcessor(true);

    public ModelAndViewResolverMethodReturnValueHandler(@Nullable List<ModelAndViewResolver> mavResolvers) {
        this.mavResolvers = mavResolvers;
    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        return true;
    }

    @Override
    public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
        if (this.mavResolvers != null) {
            for (ModelAndViewResolver mavResolver : this.mavResolvers) {
                Class<?> handlerType = returnType.getContainingClass();
                Method method = returnType.getMethod();
                Assert.state(method != null, "No handler method");
                ExtendedModelMap model = (ExtendedModelMap) mavContainer.getModel();
                ModelAndView mav = mavResolver.resolveModelAndView(method, handlerType, returnValue, model, webRequest);
                if (mav != ModelAndViewResolver.UNRESOLVED) {
                    mavContainer.addAllAttributes(mav.getModel());
                    mavContainer.setViewName(mav.getViewName());
                    if (!mav.isReference()) {
                        mavContainer.setView(mav.getView());
                    }
                    return;
                }
            }
        }
        // No suitable ModelAndViewResolver...
        if (this.modelAttributeProcessor.supportsReturnType(returnType)) {
            this.modelAttributeProcessor.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
        } else {
            throw new UnsupportedOperationException("Unexpected return type: " + returnType.getParameterType().getName() + " in method: " + returnType.getMethod());
        }
    }

}
