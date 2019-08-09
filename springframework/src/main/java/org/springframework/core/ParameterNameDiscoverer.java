package org.springframework.core;

import org.springframework.lang.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public interface ParameterNameDiscoverer {

    @Nullable
    String[] getParameterNames(Method method);

    @Nullable
    String[] getParameterNames(Constructor<?> ctor);

}
