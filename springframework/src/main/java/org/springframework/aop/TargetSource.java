package org.springframework.aop;

import org.springframework.lang.Nullable;

/**
 * 封装真实实现类的信息
 */
public interface TargetSource extends TargetClassAware {

    @Override
    @Nullable
    Class<?> getTargetClass();

    boolean isStatic();

    @Nullable
    Object getTarget() throws Exception;

    void releaseTarget(Object target) throws Exception;

}
