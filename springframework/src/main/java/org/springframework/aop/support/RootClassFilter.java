package org.springframework.aop.support;

import org.springframework.aop.ClassFilter;

import java.io.Serializable;

@SuppressWarnings("serial")
public class RootClassFilter implements ClassFilter, Serializable {

    private Class<?> clazz;

    public RootClassFilter(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public boolean matches(Class<?> candidate) {
        return this.clazz.isAssignableFrom(candidate);
    }

}
