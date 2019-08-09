package org.springframework.aop.framework;

import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.List;

public interface AdvisorChainFactory {

    List<Object> getInterceptorsAndDynamicInterceptionAdvice(Advised config, Method method, @Nullable Class<?> targetClass);

}
