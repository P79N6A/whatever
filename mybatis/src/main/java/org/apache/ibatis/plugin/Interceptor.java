package org.apache.ibatis.plugin;

import java.util.Properties;

public interface Interceptor {

    /**
     * 执行代理类方法
     */
    Object intercept(Invocation invocation) throws Throwable;

    /**
     * 包装代理对象
     */
    Object plugin(Object target);

    /**
     * 插件自定义属性
     */
    void setProperties(Properties properties);

}
