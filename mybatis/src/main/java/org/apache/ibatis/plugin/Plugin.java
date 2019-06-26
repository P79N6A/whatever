package org.apache.ibatis.plugin;

import org.apache.ibatis.reflection.ExceptionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Plugin implements InvocationHandler {

    /**
     * 被代理的目标类
     */
    private final Object target;

    /**
     * 对应的拦截器
     */
    private final Interceptor interceptor;

    /**
     * 拦截器拦截的方法缓存
     */
    private final Map<Class<?>, Set<Method>> signatureMap;

    private Plugin(Object target, Interceptor interceptor, Map<Class<?>, Set<Method>> signatureMap) {
        this.target = target;
        this.interceptor = interceptor;
        this.signatureMap = signatureMap;
    }

    /**
     * 包装的对象 拦截器
     */
    public static Object wrap(Object target, Interceptor interceptor) {

        // 从注解中获取拦截器对象的类名和方法信息
        Map<Class<?>, Set<Method>> signatureMap = getSignatureMap(interceptor);
        Class<?> type = target.getClass();
        // 解析包装的对象的所有接口
        Class<?>[] interfaces = getAllInterfaces(type, signatureMap);
        // 如果存在拦截的接口，则返回代理类，否则不处理
        if (interfaces.length > 0) {
            // 生成JDK动态代理
            return Proxy.newProxyInstance(type.getClassLoader(), interfaces, new Plugin(target, interceptor, signatureMap));
        }
        return target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            // 查询需要拦截的方法集合
            Set<Method> methods = signatureMap.get(method.getDeclaringClass());
            // 判断是否是需要拦截的方法
            if (methods != null && methods.contains(method)) {
                // 调用代理类所属拦截器的intercept方法
                return interceptor.intercept(new Invocation(target, method, args));
            }
            // 不拦截，直接调用
            return method.invoke(target, args);
        } catch (Exception e) {
            throw ExceptionUtil.unwrapThrowable(e);
        }
    }

    /**
     * 根据拦截器接口实现类的注解获取相关信息
     */
    private static Map<Class<?>, Set<Method>> getSignatureMap(Interceptor interceptor) {

        // 获得@Interceptor注解
        Intercepts interceptsAnnotation = interceptor.getClass().getAnnotation(Intercepts.class);

        if (interceptsAnnotation == null) {
            throw new PluginException("No @Intercepts annotation was found in interceptor " + interceptor.getClass().getName());
        }

        // @Signature中的type(拦截的类) method(方法) args(参数)
        Signature[] sigs = interceptsAnnotation.value();

        Map<Class<?>, Set<Method>> signatureMap = new HashMap<>();
        for (Signature sig : sigs) {
            // 第一次key不存在，创建并放入
            Set<Method> methods = signatureMap.computeIfAbsent(sig.type(), k -> new HashSet<>());
            try {
                // 获得拦截类的方法，加入到集合
                Method method = sig.type().getMethod(sig.method(), sig.args());
                methods.add(method);
            } catch (NoSuchMethodException e) {
                throw new PluginException("Could not find method on " + sig.type() + " named " + sig.method() + ". Cause: " + e, e);
            }
        }
        return signatureMap;
    }

    private static Class<?>[] getAllInterfaces(Class<?> type, Map<Class<?>, Set<Method>> signatureMap) {
        Set<Class<?>> interfaces = new HashSet<>();
        while (type != null) {
            // 获得所有接口，如果该类型在signatureMap当中则加入到set
            for (Class<?> c : type.getInterfaces()) {
                if (signatureMap.containsKey(c)) {
                    interfaces.add(c);
                }
            }
            // 父类
            type = type.getSuperclass();
        }
        // 返回接口数组
        return interfaces.toArray(new Class<?>[interfaces.size()]);
    }

}
