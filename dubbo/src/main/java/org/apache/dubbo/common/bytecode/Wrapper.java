package org.apache.dubbo.common.bytecode;

import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.demo.DemoService;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;

/*
 * public interface Car {
 *      String getBrand();
 *      long getWeight();
 *      void make(String brand, long weight);
 * }
 */

/*
 * Wrapper#makeWrapper之后生成的Wrapper子类
 *
 * public class Wrapper0 extends Wrapper {
 *     // 字段名列表
 *     public static String[] pns;
 *
 *     // 字段名与字段类型的映射关系
 *     public static java.util.Map<String, Class<?>> pts;
 *
 *     // 方法名列表
 *     public static String[] mns;
 *
 *     // 声明的方法名列表
 *     public static String[] dmns;
 *
 *     // 每个public方法的参数类型
 *     public static Class[] mts0;
 *     public static Class[] mts1;
 *     public static Class[] mts2;
 *
 *     public String[] getPropertyNames() {
 *         return pns;
 *     }
 *
 *     public boolean hasProperty(String n) {
 *         return pts.containsKey($1);
 *     }
 *
 *     public Class getPropertyType(String n) {
 *         return (Class) pts.get($1);
 *     }
 *
 *     public String[] getMethodNames() {
 *         return mns;
 *     }
 *
 *     public String[] getDeclaredMethodNames() {
 *         return dmns;
 *     }
 *
 *     public void setPropertyValue(Object o, String n, Object v) {
 *         org.apache.dubbo.common.bytecode.Car w;
 *         try {
 *             w = ((org.apache.dubbo.common.bytecode.Car) $1);
 *         } catch (Throwable e) {
 *             throw new IllegalArgumentException(e);
 *         }
 *         throw new org.apache.dubbo.common.bytecode.NoSuchPropertyException("Not found property \"" + $2 + "\" field or setter method in class org.apache.dubbo.common.bytecode.Car.");
 *     }
 *
 *     public Object getPropertyValue(Object o, String n) {
 *         org.apache.dubbo.common.bytecode.Car w;
 *         try {
 *             w = ((org.apache.dubbo.common.bytecode.Car) $1);
 *         } catch (Throwable e) {
 *             throw new IllegalArgumentException(e);
 *         }
 *         if ($2.equals("brand")) {
 *             return ($w) w.getBrand();
 *         }
 *         if ($2.equals("weight")) {
 *             return ($w) w.getWeight();
 *         }
 *         throw new org.apache.dubbo.common.bytecode.NoSuchPropertyException("Not found property \"" + $2 + "\" field or setter method in class org.apache.dubbo.common.bytecode.Car.");
 *     }
 *
 *     public Object invokeMethod(Object o, String n, Class[] p, Object[] v) throws java.lang.reflect.InvocationTargetException {
 *         org.apache.dubbo.common.bytecode.Car w;
 *         try {
 *             w = ((org.apache.dubbo.common.bytecode.Car) $1);
 *         } catch (Throwable e) {
 *             throw new IllegalArgumentException(e);
 *         }
 *         try {
 *             if ("make".equals($2) && $3.length == 2) {
 *                 w.make((java.lang.String) $4[0], ((Number) $4[1]).longValue());
 *                 return null;
 *             }
 *             if ("getBrand".equals($2) && $3.length == 0) {
 *                 return ($w) w.getBrand();
 *             }
 *             if ("getWeight".equals($2) && $3.length == 0) {
 *                 return ($w) w.getWeight();
 *             }
 *         } catch (Throwable e) {
 *             throw new java.lang.reflect.InvocationTargetException(e);
 *         }
 *         throw new org.apache.dubbo.common.bytecode.NoSuchMethodException("Not found method \"" + $2 + "\" in class org.apache.dubbo.common.bytecode.Car.");
 *     }
 * }
 *
 */

/**
 * Car是接口
 * RaceCar是Car实现类
 * ferrari和porsche是RaceCar的两个实例
 * Dubbo为接口Car生成一个Wrapper子类，比如Wrapper0；然后创建Wrapper0的实例wrapper0
 * 通过wrapper0#setPropertyValue来修改ferrari的字段，也可以修改porsche的字段
 * 通过wrapper0#invokeMethod来调用ferrari的方法，也可以调用porsche的方法
 * 优点：通过一个Wrapper0实例就可以操作N个目标接口Car的实例
 */
public abstract class Wrapper {

    private static final Map<Class<?>, Wrapper> WRAPPER_MAP = new ConcurrentHashMap<Class<?>, Wrapper>();

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final String[] OBJECT_METHODS = new String[]{"getClass", "hashCode", "toString", "equals"};

    /**
     *
     */
    private static final Wrapper OBJECT_WRAPPER = new Wrapper() {
        @Override
        public String[] getMethodNames() {
            return OBJECT_METHODS;
        }

        @Override
        public String[] getDeclaredMethodNames() {
            return OBJECT_METHODS;
        }

        @Override
        public String[] getPropertyNames() {
            return EMPTY_STRING_ARRAY;
        }

        @Override
        public Class<?> getPropertyType(String pn) {
            return null;
        }

        @Override
        public Object getPropertyValue(Object instance, String pn) throws NoSuchPropertyException {
            throw new NoSuchPropertyException("Property [" + pn + "] not found.");
        }

        @Override
        public void setPropertyValue(Object instance, String pn, Object pv) throws NoSuchPropertyException {
            throw new NoSuchPropertyException("Property [" + pn + "] not found.");
        }

        @Override
        public boolean hasProperty(String name) {
            return false;
        }

        @Override
        public Object invokeMethod(Object instance, String mn, Class<?>[] types, Object[] args) throws NoSuchMethodException {
            if ("getClass".equals(mn)) {
                return instance.getClass();
            }
            if ("hashCode".equals(mn)) {
                return instance.hashCode();
            }
            if ("toString".equals(mn)) {
                return instance.toString();
            }
            if ("equals".equals(mn)) {
                if (args.length == 1) {
                    return instance.equals(args[0]);
                }
                throw new IllegalArgumentException("Invoke method [" + mn + "] argument number error.");
            }
            throw new NoSuchMethodException("Method [" + mn + "] not found.");
        }
    };

    private static AtomicLong WRAPPER_CLASS_COUNTER = new AtomicLong(0);

    public static Wrapper getWrapper(Class<?> c) {
        while (ClassGenerator.isDynamicClass(c)) {
            c = c.getSuperclass();
        }
        if (c == Object.class) {
            return OBJECT_WRAPPER;
        }
        // 从缓存中获取Wrapper实例
        Wrapper ret = WRAPPER_MAP.get(c);
        if (ret == null) {
            // 缓存未命中，创建Wrapper
            ret = makeWrapper(c);
            // 写入缓存
            WRAPPER_MAP.put(c, ret);
        }
        return ret;
    }

    private static Wrapper makeWrapper(Class<?> c) {
        // 检测是否为基本类型，若是则抛出异常
        if (c.isPrimitive()) {
            throw new IllegalArgumentException("Can not create wrapper for primitive type: " + c);
        }

        /*
         * 初始化操作
         */
        String name = c.getName();
        ClassLoader cl = ClassUtils.getClassLoader(c);
        // setPropertyValue
        StringBuilder c1 = new StringBuilder("public void setPropertyValue(Object o, String n, Object v){ ");
        // getPropertyValue
        StringBuilder c2 = new StringBuilder("public Object getPropertyValue(Object o, String n){ ");
        // invokeMethod
        StringBuilder c3 = new StringBuilder("public Object invokeMethod(Object o, String n, Class[] p, Object[] v) throws " + InvocationTargetException.class.getName() + "{ ");
        // 生成类型转换代码及异常捕捉代码，比如：
        // DemoService w;
        // try {
        //     w = ((DemoService) $1);
        // } catch(Throwable e) {
        //     throw new IllegalArgumentException(e);
        // }
        c1.append(name).append(" w; try{ w = ((").append(name).append(")$1); }catch(Throwable e){ throw new IllegalArgumentException(e); }");
        c2.append(name).append(" w; try{ w = ((").append(name).append(")$1); }catch(Throwable e){ throw new IllegalArgumentException(e); }");
        c3.append(name).append(" w; try{ w = ((").append(name).append(")$1); }catch(Throwable e){ throw new IllegalArgumentException(e); }");
        // 存储成员变量名和类型
        Map<String, Class<?>> pts = new HashMap<>();
        // 存储方法描述信息（可理解为方法签名）及Method实例
        Map<String, Method> ms = new LinkedHashMap<>();
        // 方法名列表
        List<String> mns = new ArrayList<>();
        // 存储“定义在当前类中的方法”的名称
        List<String> dmns = new ArrayList<>();
        // --------------------------------✨ 分割线1 ✨-------------------------------------
        /*
         * 为public级别的字段生成条件判断取值与赋值代码
         */
        // 获取public访问级别的字段，并为所有字段生成条件判断语句
        for (Field f : c.getFields()) {
            String fn = f.getName();
            Class<?> ft = f.getType();
            if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) {
                // 忽略static或transient修饰的变量
                continue;
            }
            // 生成条件判断及赋值语句，比如：
            // if ($2.equals("name")) {
            //     w.name = (java.lang.String) $3;
            //     return;
            // }
            // if ($2.equals("age")) {
            //     w.age = ((Number) $3).intValue();
            //     return;
            // }
            c1.append(" if( $2.equals(\"").append(fn).append("\") ){ w.").append(fn).append("=").append(arg(ft, "$3")).append("; return; }");
            // 生成条件判断及返回语句，比如：
            // if ($2.equals("name")) {
            //     return ($w) w.name;
            // }
            c2.append(" if( $2.equals(\"").append(fn).append("\") ){ return ($w)w.").append(fn).append("; }");
            // 存储<字段名, 字段类型>键值对到pts
            pts.put(fn, ft);
        }
        // --------------------------------✨ 分割线2 ✨-------------------------------------
        /*
         * 为定义在当前类中的方法生成判断语句，和方法调用语句
         */
        Method[] methods = c.getMethods();
        // 检测c中是否包含在当前类中声明的方法
        boolean hasMethod = hasMethods(methods);
        if (hasMethod) {
            c3.append(" try{");
            for (Method m : methods) {
                if (m.getDeclaringClass() == Object.class) {
                    // 忽略Object中定义的方法
                    continue;
                }
                String mn = m.getName();
                // 生成方法名判断语句，比如：
                // if ( "sayHello".equals( $2 )
                c3.append(" if( \"").append(mn).append("\".equals( $2 ) ");
                int len = m.getParameterTypes().length;
                // 生成“运行时传入的参数数量与方法参数列表长度”判断语句，比如：
                // && $3.length == 2
                c3.append(" && ").append(" $3.length == ").append(len);
                boolean override = false;
                for (Method m2 : methods) {
                    // 检测方法是否存在重载情况，条件为：方法对象不同 && 方法名相同
                    if (m != m2 && m.getName().equals(m2.getName())) {
                        override = true;
                        break;
                    }
                }
                // 对重载方法进行处理，考虑下面的方法：
                //    1. void sayHello(Integer, String)
                //    2. void sayHello(Integer, Integer)
                // 方法名相同，参数列表长度也相同，需要进一步判断方法的参数类型
                if (override) {
                    if (len > 0) {
                        for (int l = 0; l < len; l++) {
                            // 生成参数类型进行检测代码，比如：
                            // && $3[0].getName().equals("java.lang.Integer")
                            //    && $3[1].getName().equals("java.lang.String")
                            c3.append(" && ").append(" $3[").append(l).append("].getName().equals(\"").append(m.getParameterTypes()[l].getName()).append("\")");
                        }
                    }
                }
                // 添加 ) {，完成方法判断语句，此时生成的代码可能如下（已格式化）：
                // if ("sayHello".equals($2)
                //     && $3.length == 2
                //     && $3[0].getName().equals("java.lang.Integer")
                //     && $3[1].getName().equals("java.lang.String")) {
                c3.append(" ) { ");
                // 根据返回值类型生成目标方法调用语句
                if (m.getReturnType() == Void.TYPE) {
                    // w.sayHello((java.lang.Integer)$4[0], (java.lang.String)$4[1]); return null;
                    c3.append(" w.").append(mn).append('(').append(args(m.getParameterTypes(), "$4")).append(");").append(" return null;");
                } else {
                    // return w.sayHello((java.lang.Integer)$4[0], (java.lang.String)$4[1]);
                    c3.append(" return ($w)w.").append(mn).append('(').append(args(m.getParameterTypes(), "$4")).append(");");
                }
                // 添加 }, 生成的代码形如（已格式化）：
                // if ("sayHello".equals($2)
                //     && $3.length == 2
                //     && $3[0].getName().equals("java.lang.Integer")
                //     && $3[1].getName().equals("java.lang.String")) {
                //
                //     w.sayHello((java.lang.Integer)$4[0], (java.lang.String)$4[1]);
                //     return null;
                // }
                c3.append(" }");
                // 添加方法名到mns集合中
                mns.add(mn);
                // 检测当前方法是否在c中被声明的
                if (m.getDeclaringClass() == c) {
                    // 若是，则将当前方法名添加到dmns
                    dmns.add(mn);
                }
                ms.put(ReflectUtils.getDesc(m), m);
            }
            // 添加异常捕捉语句
            c3.append(" } catch(Throwable e) { ");
            c3.append("     throw new java.lang.reflect.InvocationTargetException(e); ");
            c3.append(" }");
        }
        // 添加NoSuchMethodException异常抛出代码
        c3.append(" throw new " + NoSuchMethodException.class.getName() + "(\"Not found method \\\"\"+$2+\"\\\" in class " + c.getName() + ".\"); }");
        // --------------------------------✨ 分割线3 ✨-------------------------------------
        /*
         * 处理getter、setter及以is/has/can开头的方法
         * 处理方式是通过正则表达式获取方法类型（get/set/is/...），以及属性名，之后为属性名生成判断语句，然后为方法生成调用语句
         */
        Matcher matcher;
        // 处理get/set方法
        for (Map.Entry<String, Method> entry : ms.entrySet()) {
            String md = entry.getKey();
            Method method = entry.getValue();
            // 以get开头的方法
            if ((matcher = ReflectUtils.GETTER_METHOD_DESC_PATTERN.matcher(md)).matches()) {
                // 获取属性名
                String pn = propertyName(matcher.group(1));
                // 生成属性判断以及返回语句，示例如下：
                // if( $2.equals("name") ) { return ($w).w.getName(); }
                c2.append(" if( $2.equals(\"").append(pn).append("\") ){ return ($w)w.").append(method.getName()).append("(); }");
                pts.put(pn, method.getReturnType());
            }
            // 以is/has/can开头的方法
            else if ((matcher = ReflectUtils.IS_HAS_CAN_METHOD_DESC_PATTERN.matcher(md)).matches()) {
                String pn = propertyName(matcher.group(1));
                // 生成属性判断以及返回语句，示例如下：
                // if( $2.equals("dream") ) { return ($w).w.hasDream(); }
                c2.append(" if( $2.equals(\"").append(pn).append("\") ){ return ($w)w.").append(method.getName()).append("(); }");
                pts.put(pn, method.getReturnType());
            }
            // 以set开头的方法
            else if ((matcher = ReflectUtils.SETTER_METHOD_DESC_PATTERN.matcher(md)).matches()) {
                Class<?> pt = method.getParameterTypes()[0];
                String pn = propertyName(matcher.group(1));
                // 生成属性判断以及setter调用语句，示例如下：
                // if( $2.equals("name") ) { w.setName((java.lang.String)$3); return; }
                c1.append(" if( $2.equals(\"").append(pn).append("\") ){ w.").append(method.getName()).append("(").append(arg(pt, "$3")).append("); return; }");
                pts.put(pn, pt);
            }
        }
        // 添加NoSuchPropertyException异常抛出代码
        c1.append(" throw new " + NoSuchPropertyException.class.getName() + "(\"Not found property \\\"\"+$2+\"\\\" field or setter method in class " + c.getName() + ".\"); }");
        c2.append(" throw new " + NoSuchPropertyException.class.getName() + "(\"Not found property \\\"\"+$2+\"\\\" field or setter method in class " + c.getName() + ".\"); }");
        // --------------------------------✨ 分割线4 ✨-------------------------------------
        /*
         * 通过ClassGenerator为刚刚生成的代码构建Class类，通过反射创建对象，通过javassist构建Class
         */
        long id = WRAPPER_CLASS_COUNTER.getAndIncrement();
        // 创建类生成器
        ClassGenerator cc = ClassGenerator.newInstance(cl);
        // 设置类名及超类
        cc.setClassName((Modifier.isPublic(c.getModifiers()) ? Wrapper.class.getName() : c.getName() + "$sw") + id);
        cc.setSuperClass(Wrapper.class);
        // 添加默认构造方法
        cc.addDefaultConstructor();
        // 添加字段
        cc.addField("public static String[] pns;");
        cc.addField("public static " + Map.class.getName() + " pts;");
        cc.addField("public static String[] mns;");
        cc.addField("public static String[] dmns;");
        for (int i = 0, len = ms.size(); i < len; i++) {
            cc.addField("public static Class[] mts" + i + ";");
        }
        // 添加方法代码
        cc.addMethod("public String[] getPropertyNames(){ return pns; }");
        cc.addMethod("public boolean hasProperty(String n){ return pts.containsKey($1); }");
        cc.addMethod("public Class getPropertyType(String n){ return (Class)pts.get($1); }");
        cc.addMethod("public String[] getMethodNames(){ return mns; }");
        cc.addMethod("public String[] getDeclaredMethodNames(){ return dmns; }");
        cc.addMethod(c1.toString());
        cc.addMethod(c2.toString());
        cc.addMethod(c3.toString());
        try {
            // 生成类
            Class<?> wc = cc.toClass();
            // 设置字段值
            wc.getField("pts").set(null, pts);
            wc.getField("pns").set(null, pts.keySet().toArray(new String[0]));
            wc.getField("mns").set(null, mns.toArray(new String[0]));
            wc.getField("dmns").set(null, dmns.toArray(new String[0]));
            int ix = 0;
            for (Method m : ms.values()) {
                wc.getField("mts" + ix++).set(null, m.getParameterTypes());
            }
            // 创建Wrapper实例
            return (Wrapper) wc.newInstance();
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            cc.release();
            ms.clear();
            mns.clear();
            dmns.clear();
        }

    }

    /**
     * 转型代码
     */
    private static String arg(Class<?> cl, String name) {
        if (cl.isPrimitive()) {
            if (cl == Boolean.TYPE) {
                return "((Boolean)" + name + ").booleanValue()";
            }
            if (cl == Byte.TYPE) {
                return "((Byte)" + name + ").byteValue()";
            }
            if (cl == Character.TYPE) {
                return "((Character)" + name + ").charValue()";
            }
            if (cl == Double.TYPE) {
                return "((Number)" + name + ").doubleValue()";
            }
            if (cl == Float.TYPE) {
                return "((Number)" + name + ").floatValue()";
            }
            if (cl == Integer.TYPE) {
                return "((Number)" + name + ").intValue()";
            }
            if (cl == Long.TYPE) {
                return "((Number)" + name + ").longValue()";
            }
            if (cl == Short.TYPE) {
                return "((Number)" + name + ").shortValue()";
            }
            throw new RuntimeException("Unknown primitive type: " + cl.getName());
        }
        return "(" + ReflectUtils.getName(cl) + ")" + name;
    }

    private static String args(Class<?>[] cs, String name) {
        int len = cs.length;
        if (len == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(arg(cs[i], name + "[" + i + "]"));
        }
        return sb.toString();
    }

    private static String propertyName(String pn) {
        return pn.length() == 1 || Character.isLowerCase(pn.charAt(1)) ? Character.toLowerCase(pn.charAt(0)) + pn.substring(1) : pn;
    }

    private static boolean hasMethods(Method[] methods) {
        if (methods == null || methods.length == 0) {
            return false;
        }
        for (Method m : methods) {
            if (m.getDeclaringClass() != Object.class) {
                return true;
            }
        }
        return false;
    }

    abstract public String[] getPropertyNames();

    abstract public Class<?> getPropertyType(String pn);

    abstract public boolean hasProperty(String name);

    abstract public Object getPropertyValue(Object instance, String pn) throws NoSuchPropertyException, IllegalArgumentException;

    abstract public void setPropertyValue(Object instance, String pn, Object pv) throws NoSuchPropertyException, IllegalArgumentException;

    public Object[] getPropertyValues(Object instance, String[] pns) throws NoSuchPropertyException, IllegalArgumentException {
        Object[] ret = new Object[pns.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = getPropertyValue(instance, pns[i]);
        }
        return ret;
    }

    public void setPropertyValues(Object instance, String[] pns, Object[] pvs) throws NoSuchPropertyException, IllegalArgumentException {
        if (pns.length != pvs.length) {
            throw new IllegalArgumentException("pns.length != pvs.length");
        }
        for (int i = 0; i < pns.length; i++) {
            setPropertyValue(instance, pns[i], pvs[i]);
        }
    }

    abstract public String[] getMethodNames();

    abstract public String[] getDeclaredMethodNames();

    public boolean hasMethod(String name) {
        for (String mn : getMethodNames()) {
            if (mn.equals(name)) {
                return true;
            }
        }
        return false;
    }

    abstract public Object invokeMethod(Object instance, String mn, Class<?>[] types, Object[] args) throws NoSuchMethodException, InvocationTargetException;

}

