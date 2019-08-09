package org.springframework.core;

import org.springframework.lang.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

public class PrioritizedParameterNameDiscoverer implements ParameterNameDiscoverer {

    private final List<ParameterNameDiscoverer> parameterNameDiscoverers = new LinkedList<>();

    public void addDiscoverer(ParameterNameDiscoverer pnd) {
        this.parameterNameDiscoverers.add(pnd);
    }

    @Override
    @Nullable
    public String[] getParameterNames(Method method) {
        for (ParameterNameDiscoverer pnd : this.parameterNameDiscoverers) {
            String[] result = pnd.getParameterNames(method);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    @Nullable
    public String[] getParameterNames(Constructor<?> ctor) {
        for (ParameterNameDiscoverer pnd : this.parameterNameDiscoverers) {
            String[] result = pnd.getParameterNames(ctor);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

}
