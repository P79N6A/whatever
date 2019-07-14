package org.apache.dubbo.common.bytecode;

import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.ReflectUtils;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.dubbo.common.constants.CommonConstants.MAX_PROXY_COUNT;
/**
 * public class proxy0 implements ClassGenerator.DC, EchoService, DemoService {
 *     // 方法数组
 *     public static Method[] methods;
 *     private InvocationHandler handler;
 *
 *     public proxy0(InvocationHandler invocationHandler) {
 *         this.handler = invocationHandler;
 *     }
 *
 *     public proxy0() {
 *     }
 *
 *     public String sayHello(String string) {
 *         // 将参数存储到Object数组中
 *         Object[] arrobject = new Object[]{string};
 *         // 调用InvocationHandler实现类的invoke方法得到调用结果
 *         Object object = this.handler.invoke(this, methods[0], arrobject);
 *         // 返回调用结果
 *         return (String)object;
 *     }
 *
 *     public Object $echo(Object object) {
 *         Object[] arrobject = new Object[]{object};
 *         Object object2 = this.handler.invoke(this, methods[1], arrobject);
 *         return object2;
 *     }
 * }
 */
public abstract class Proxy {
    public static final InvocationHandler RETURN_NULL_INVOKER = (proxy, method, args) -> null;

    public static final InvocationHandler THROW_UNSUPPORTED_INVOKER = new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            throw new UnsupportedOperationException("Method [" + ReflectUtils.getName(method) + "] unimplemented.");
        }
    };

    private static final AtomicLong PROXY_CLASS_COUNTER = new AtomicLong(0);

    private static final String PACKAGE_NAME = Proxy.class.getPackage().getName();

    private static final Map<ClassLoader, Map<String, Object>> PROXY_CACHE_MAP = new WeakHashMap<ClassLoader, Map<String, Object>>();

    private static final Object PENDING_GENERATION_MARKER = new Object();

    protected Proxy() {
    }

    public static Proxy getProxy(Class<?>... ics) {
        // 调用重载方法
        return getProxy(ClassUtils.getClassLoader(Proxy.class), ics);
    }

    public static Proxy getProxy(ClassLoader cl, Class<?>... ics) {
        if (ics.length > MAX_PROXY_COUNT) {
            throw new IllegalArgumentException("interface limit exceeded");
        }
        StringBuilder sb = new StringBuilder();
        // 遍历接口列表
        for (int i = 0; i < ics.length; i++) {
            String itf = ics[i].getName();
            // 检测类型是否为接口
            if (!ics[i].isInterface()) {
                throw new RuntimeException(itf + " is not a interface.");
            }
            Class<?> tmp = null;
            try {
                // 重新加载接口类
                tmp = Class.forName(itf, false, cl);
            } catch (ClassNotFoundException e) {
            }
            // 检测接口是否相同，这里tmp有可能为空
            if (tmp != ics[i]) {
                throw new IllegalArgumentException(ics[i] + " is not visible from class loader");
            }
            // 拼接接口全限定名，分隔符为;
            sb.append(itf).append(';');
        }
        // 使用拼接后的接口名作为key
        String key = sb.toString();
        Map<String, Object> cache;
        synchronized (PROXY_CACHE_MAP) {
            cache = PROXY_CACHE_MAP.computeIfAbsent(cl, k -> new HashMap<>());
        }
        Proxy proxy = null;
        synchronized (cache) {
            do {
                // 从缓存中获取Reference<Proxy>实例
                Object value = cache.get(key);
                if (value instanceof Reference<?>) {
                    proxy = (Proxy) ((Reference<?>) value).get();
                    if (proxy != null) {
                        return proxy;
                    }
                }
                // 并发控制，保证只有一个线程可以进行后续操作
                if (value == PENDING_GENERATION_MARKER) {
                    try {
                        // 其他线程在此处进行等待
                        cache.wait();
                    } catch (InterruptedException e) {
                    }
                } else {
                    // 放置标志位到缓存中，并跳出while循环进行后续操作
                    cache.put(key, PENDING_GENERATION_MARKER);
                    break;
                }
            } while (true);
        }
        long id = PROXY_CLASS_COUNTER.getAndIncrement();
        String pkg = null;

        /*
         * ccp：为服务接口生成代理类，比如有一个DemoService接口，这个接口代理类就是由ccp生成的
         * ccm：为org.apache.dubbo.common.bytecode.Proxy抽象类生成子类，主要是实现Proxy类的抽象方法
         */
        ClassGenerator ccp = null, ccm = null;
        try {
            // 创建ClassGenerator对象
            ccp = ClassGenerator.newInstance(cl);
            Set<String> worked = new HashSet<>();
            List<Method> methods = new ArrayList<>();
            for (int i = 0; i < ics.length; i++) {
                // 接口访问级别不是public
                if (!Modifier.isPublic(ics[i].getModifiers())) {
                    // 获取接口包名
                    String npkg = ics[i].getPackage().getName();
                    if (pkg == null) {
                        pkg = npkg;
                    } else {
                        if (!pkg.equals(npkg)) {
                            // 非public级别的接口必须在同一个包下，否者抛出异常
                            throw new IllegalArgumentException("non-public interfaces from different packages");
                        }
                    }
                }
                // 添加接口到ClassGenerator中
                ccp.addInterface(ics[i]);
                // 遍历接口方法
                for (Method method : ics[i].getMethods()) {
                    // 获取方法描述，可理解为方法签名
                    String desc = ReflectUtils.getDesc(method);
                    // 如果方法描述字符串已在worked中，则忽略，A接口和B接口中包含一个完全相同的方法
                    if (worked.contains(desc)) {
                        continue;
                    }
                    worked.add(desc);
                    int ix = methods.size();
                    // 获取方法返回值类型
                    Class<?> rt = method.getReturnType();
                    // 获取参数列表
                    Class<?>[] pts = method.getParameterTypes();
                    // 生成Object[] args = new Object[1...N]
                    StringBuilder code = new StringBuilder("Object[] args = new Object[").append(pts.length).append("];");
                    for (int j = 0; j < pts.length; j++) {
                        // 生成 args[1...N] = ($w)$1...N;
                        code.append(" args[").append(j).append("] = ($w)$").append(j + 1).append(";");
                    }
                    // 生成InvokerHandler接口的invoker方法调用语句，如下：
                    // Object ret = handler.invoke(this, methods[1...N], args);
                    code.append(" Object ret = handler.invoke(this, methods[").append(ix).append("], args);");
                    // 返回值不为void
                    if (!Void.TYPE.equals(rt)) {
                        // 生成返回语句，如：return (java.lang.String) ret;
                        code.append(" return ").append(asArgument(rt, "ret")).append(";");
                    }
                    methods.add(method);
                    // 添加方法名、访问控制符、参数列表、方法代码等信息到ClassGenerator
                    ccp.addMethod(method.getName(), method.getModifiers(), rt, pts, method.getExceptionTypes(), code.toString());
                }
            }
            if (pkg == null) {
                pkg = PACKAGE_NAME;
            }
            // 构建接口代理类名称：pkg + ".proxy" + id，比如 org.apache.dubbo.proxy0
            String pcn = pkg + ".proxy" + id;
            ccp.setClassName(pcn);
            ccp.addField("public static java.lang.reflect.Method[] methods;");
            // 生成：private java.lang.reflect.InvocationHandler handler;
            ccp.addField("private " + InvocationHandler.class.getName() + " handler;");
            // 为接口代理类添加带有InvocationHandler参数的构造方法，比如：
            // porxy0(java.lang.reflect.InvocationHandler arg0) {
            //     handler=$1;
            // }
            ccp.addConstructor(Modifier.PUBLIC, new Class<?>[]{InvocationHandler.class}, new Class<?>[0], "handler=$1;");
            // 为接口代理类添加默认构造方法
            ccp.addDefaultConstructor();
            // 生成接口代理类
            Class<?> clazz = ccp.toClass();
            clazz.getField("methods").set(null, methods.toArray(new Method[0]));
            /*
             * 以org.apache.dubbo.demo.DemoService这个接口为例
             *
             * package org.apache.dubbo.common.bytecode;
             *
             * public class proxy0 implements org.apache.dubbo.demo.DemoService {
             *
             *     public static java.lang.reflect.Method[] methods;
             *
             *     private java.lang.reflect.InvocationHandler handler;
             *
             *     public proxy0() {
             *     }
             *
             *     public proxy0(java.lang.reflect.InvocationHandler arg0) {
             *         handler = $1;
             *     }
             *
             *     public java.lang.String sayHello(java.lang.String arg0) {
             *         Object[] args = new Object[1];
             *         args[0] = ($w) $1;
             *         Object ret = handler.invoke(this, methods[0], args);
             *         return (java.lang.String) ret;
             *     }
             * }
             * */


            // 构建Proxy子类名称，比如Proxy1，Proxy2
            String fcn = Proxy.class.getName() + id;
            ccm = ClassGenerator.newInstance(cl);
            ccm.setClassName(fcn);
            ccm.addDefaultConstructor();
            ccm.setSuperClass(Proxy.class);
            // 为Proxy的抽象方法newInstance生成实现代码，形如：
            // public Object newInstance(java.lang.reflect.InvocationHandler h) {
            //     return new org.apache.dubbo.proxy0($1);
            // }
            ccm.addMethod("public Object newInstance(" + InvocationHandler.class.getName() + " h){ return new " + pcn + "($1); }");
            // 生成Proxy实现类
            Class<?> pc = ccm.toClass();
            // 通过反射创建Proxy实例
            proxy = (Proxy) pc.newInstance();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (ccp != null) {
                // 释放资源
                ccp.release();
            }
            if (ccm != null) {
                ccm.release();
            }
            synchronized (cache) {
                if (proxy == null) {
                    cache.remove(key);
                } else {
                    // 写缓存
                    cache.put(key, new WeakReference<Proxy>(proxy));
                }
                // 唤醒其他等待线程
                cache.notifyAll();
            }
        }


        return proxy;
    }

    private static String asArgument(Class<?> cl, String name) {
        if (cl.isPrimitive()) {
            if (Boolean.TYPE == cl) {
                return name + "==null?false:((Boolean)" + name + ").booleanValue()";
            }
            if (Byte.TYPE == cl) {
                return name + "==null?(byte)0:((Byte)" + name + ").byteValue()";
            }
            if (Character.TYPE == cl) {
                return name + "==null?(char)0:((Character)" + name + ").charValue()";
            }
            if (Double.TYPE == cl) {
                return name + "==null?(double)0:((Double)" + name + ").doubleValue()";
            }
            if (Float.TYPE == cl) {
                return name + "==null?(float)0:((Float)" + name + ").floatValue()";
            }
            if (Integer.TYPE == cl) {
                return name + "==null?(int)0:((Integer)" + name + ").intValue()";
            }
            if (Long.TYPE == cl) {
                return name + "==null?(long)0:((Long)" + name + ").longValue()";
            }
            if (Short.TYPE == cl) {
                return name + "==null?(short)0:((Short)" + name + ").shortValue()";
            }
            throw new RuntimeException(name + " is unknown primitive type.");
        }
        return "(" + ReflectUtils.getName(cl) + ")" + name;
    }

    public Object newInstance() {
        return newInstance(THROW_UNSUPPORTED_INVOKER);
    }

    abstract public Object newInstance(InvocationHandler handler);

}
