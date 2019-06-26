package org.apache.ibatis.test;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;

import java.util.Properties;

@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
public class ExamplePlugin implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        final Executor executor = (Executor) invocation.getTarget();
        final Object[] args = invocation.getArgs();
        final MappedStatement mappedStatement = (MappedStatement) args[0];
        final Object object = args[1];
        // 调用下一个拦截器拦截目标方法
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        // 判断目标类型，减少被代理的次数
        if (target instanceof Executor) {
            return Plugin.wrap(target, this);
        } else {
            return target;
        }

    }

    @Override
    public void setProperties(Properties properties) {
        // 设置额外的参数，参数配置在拦截器的Properties节点里
    }
}