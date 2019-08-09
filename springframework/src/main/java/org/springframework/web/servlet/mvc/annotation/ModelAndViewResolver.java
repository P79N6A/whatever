package org.springframework.web.servlet.mvc.annotation;

import org.springframework.lang.Nullable;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.ModelAndView;

import java.lang.reflect.Method;

public interface ModelAndViewResolver {

    ModelAndView UNRESOLVED = new ModelAndView();

    ModelAndView resolveModelAndView(Method handlerMethod, Class<?> handlerType, @Nullable Object returnValue, ExtendedModelMap implicitModel, NativeWebRequest webRequest);

}
