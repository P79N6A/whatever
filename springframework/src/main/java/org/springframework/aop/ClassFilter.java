package org.springframework.aop;

@FunctionalInterface
public interface ClassFilter {

    boolean matches(Class<?> clazz);

    ClassFilter TRUE = TrueClassFilter.INSTANCE;

}
