package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.lang.Nullable;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.InitBinderDataBinderFactory;
import org.springframework.web.method.support.InvocableHandlerMethod;

import java.util.List;

public class ServletRequestDataBinderFactory extends InitBinderDataBinderFactory {

    public ServletRequestDataBinderFactory(@Nullable List<InvocableHandlerMethod> binderMethods, @Nullable WebBindingInitializer initializer) {
        super(binderMethods, initializer);
    }

    @Override
    protected ServletRequestDataBinder createBinderInstance(@Nullable Object target, String objectName, NativeWebRequest request) throws Exception {
        return new ExtendedServletRequestDataBinder(target, objectName);
    }

}
