package org.apache.dubbo.common.extension;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.support.ActivateComparator;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import static org.apache.dubbo.common.constants.CommonConstants.*;

public class ExtensionLoader<T> {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionLoader.class);

    private static final String SERVICES_DIRECTORY = "META-INF/services/";

    private static final String DUBBO_DIRECTORY = "META-INF/dubbo/";

    private static final String DUBBO_INTERNAL_DIRECTORY = DUBBO_DIRECTORY + "internal/";

    private static final Pattern NAME_SEPARATOR = Pattern.compile("\\s*[,]+\\s*");

    /**
     * SPI扩展点接口Class - ExtensionLoader实例对象
     */
    private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();

    /**
     * SPI扩展实现类Class - SPI扩展实现类实例对象（非@Adaptive && 非Wrapper）
     */
    private static final ConcurrentMap<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();

    /**
     * 被@SPI注解的接口Class，即扩展点
     */
    private final Class<?> type;

    /**
     * 扩展工厂，从中获取扩展类型实例对象，缺省为AdaptiveExtensionFactory，注入依赖用
     */
    private final ExtensionFactory objectFactory;

    /**
     * 非Wrapper装饰模式（不存在只有一个参数，并且参数是扩展点类型实例对象的构造函数） && 类没被@Adaptive注解的扩展实现类Class - 扩展名
     */
    private final ConcurrentMap<Class<?>, String> cachedNames = new ConcurrentHashMap<>();

    /**
     * 扩展名 - 非Wrapper装饰模式 && 类没被@Adaptive注解的扩展实现类Class
     */
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();

    /**
     * 扩展名 - 非Wrapper装饰模式 && 类没被@Adaptive注解 && 类被@Activate注解的@Adaptive实例对象
     */
    private final Map<String, Object> cachedActivates = new ConcurrentHashMap<>();

    /**
     * 扩展名 - 已加载的Holder实例对象（非@Adaptive && 非Wrapper）
     */
    private final ConcurrentMap<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();

    /**
     * 类被@Adpative注解的扩展实现类实例Holder对象，只能有一个
     */
    private final Holder<Object> cachedAdaptiveInstance = new Holder<>();

    /**
     * 类被@Adpative注解的扩展实现类Class，只能有一个
     */
    private volatile Class<?> cachedAdaptiveClass = null;

    /**
     * 被@SPI注解指定的缺省扩展名
     */
    private String cachedDefaultName;

    /**
     * 创建适配扩展实例过程中抛出的异常
     */
    private volatile Throwable createAdaptiveInstanceError;

    /**
     * 满足装饰模式的扩展类Class
     */
    private Set<Class<?>> cachedWrapperClasses;

    /**
     * 扩展点配置文件的一行 - 异常
     */
    private Map<String, IllegalStateException> exceptions = new ConcurrentHashMap<>();

    private ExtensionLoader(Class<?> type) {
        this.type = type;
        /*
         * 返回AdaptiveExtensionFactory，内部维护了一个ExtensionFactory列表，用于存储其他类型的ExtensionFactory
         * Dubbo目前提供了两种ExtensionFactory，分别是SpiExtensionFactory和SpringExtensionFactory
         * 前者用于创建自适应的拓展，后者是用于从Spring的IOC容器中获取所需的拓展，作用类似IOC
         */
        objectFactory = (type == ExtensionFactory.class ? null : ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getAdaptiveExtension());
    }

    private static <T> boolean withExtensionAnnotation(Class<T> type) {
        return type.isAnnotationPresent(SPI.class);
    }

    @SuppressWarnings("unchecked")
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        // 非空判断
        if (type == null) {
            throw new IllegalArgumentException("Extension type == null");
        }
        // @SPI拓展点类型只能是接口
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type (" + type + ") is not an interface!");
        }
        // 没有@SPI注解
        if (!withExtensionAnnotation(type)) {
            throw new IllegalArgumentException("Extension type (" + type + ") is not an extension, because it is NOT annotated with @" + SPI.class.getSimpleName() + "!");
        }
        // 从缓存EXTENSION_LOADERS中获取，如果不存在则新建后加入缓存
        ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        if (loader == null) {
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<T>(type));
            loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        }
        return loader;
    }

    public static void resetExtensionLoader(Class type) {
        ExtensionLoader loader = EXTENSION_LOADERS.get(type);
        if (loader != null) {
            Map<String, Class<?>> classes = loader.getExtensionClasses();
            for (Map.Entry<String, Class<?>> entry : classes.entrySet()) {
                EXTENSION_INSTANCES.remove(entry.getValue());
            }
            classes.clear();
            EXTENSION_LOADERS.remove(type);
        }
    }

    private static ClassLoader findClassLoader() {
        return ClassUtils.getClassLoader(ExtensionLoader.class);
    }

    public String getExtensionName(T extensionInstance) {
        return getExtensionName(extensionInstance.getClass());
    }

    public String getExtensionName(Class<?> extensionClass) {
        getExtensionClasses();
        return cachedNames.get(extensionClass);
    }

    public List<T> getActivateExtension(URL url, String key) {
        return getActivateExtension(url, key, null);
    }

    public List<T> getActivateExtension(URL url, String[] values) {
        return getActivateExtension(url, values, null);
    }

    public List<T> getActivateExtension(URL url, String key, String group) {
        String value = url.getParameter(key);
        return getActivateExtension(url, StringUtils.isEmpty(value) ? null : COMMA_SPLIT_PATTERN.split(value), group);
    }

    /**
     * 被@Activate注解的
     */
    public List<T> getActivateExtension(URL url, String[] values, String group) {
        logger.debug("url:" + url);
        logger.debug("values:" + values);
        List<T> exts = new ArrayList<>();
        // 将传过来的values包装成List，如自定义的filter
        List<String> names = values == null ? new ArrayList<>(0) : Arrays.asList(values);
        // 包装好的数据中不包含"-default"，就是默认加载所有的默认filter
        if (!names.contains(REMOVE_VALUE_PREFIX + DEFAULT_KEY)) {
            // 加载所有拓展
            getExtensionClasses();
            // 类上无@Adaptive && 非Wrapper && 类被@Activate注解
            for (Map.Entry<String, Object> entry : cachedActivates.entrySet()) {
                // 扩展名称
                String name = entry.getKey();
                // 扩展类的@Activate注解
                Object activate = entry.getValue();
                String[] activateGroup, activateValue;
                if (activate instanceof Activate) {
                    activateGroup = ((Activate) activate).group();
                    activateValue = ((Activate) activate).value();
                } else {
                    // 不存在这种情况吧
                    continue;
                }
                // 判断group是否属于范围，比如区分是在provider端生效还是consumer端生效
                if (isMatchGroup(group, activateGroup)) {
                    // 从缓存中获取此name对应的实例
                    T ext = getExtension(name);
                    // 1. names不包含name（用户配置的filter列表中不包含当前ext）
                    // 2. names中不包含"-default"（用户配置的filter列表中不包含当前ext的加-的key）
                    // 3. 通过URL判断这个注解是激活的（如果配置信息中有可以激活的配置key并且数据不为0,false,null，N/A）
                    if (!names.contains(name) && !names.contains(REMOVE_VALUE_PREFIX + name) && isActive(activateValue, url)) {
                        // 增加扩展
                        exts.add(ext);
                    }
                }
            }
            // 根据@Activate注解上的order排序
            exts.sort(ActivateComparator.COMPARATOR);
        }
        // 以上为加载原生filter，下面处理自定义Filter
        // 临时变量
        List<T> usrs = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            // 不以-开头 && 不含-'name'
            if (!name.startsWith(REMOVE_VALUE_PREFIX) && !names.contains(REMOVE_VALUE_PREFIX + name)) {
                // 通过default关键字替换原生的Filter链，主要用来控制调用链顺序
                if (DEFAULT_KEY.equals(name)) {
                    if (!usrs.isEmpty()) {
                        // 添加到头部
                        exts.addAll(0, usrs);
                        // 清空
                        usrs.clear();
                    }
                } else {
                    T ext = getExtension(name);
                    // 加入自定义的扩展Filter
                    usrs.add(ext);
                }
            }
        }
        if (!usrs.isEmpty()) {
            exts.addAll(usrs);
        }
        return exts;
        /*
         * <dubbo:reference filter="filter1,filter2"/>: 执行顺序为, 原生filter子链->filter1->filter2
         * <dubbo:reference filter="filter1,filter2,default"/>: 执行顺序为, filter1->filter2->原生filter子链
         * <dubbo:service filter="filter1,default,filter2,-xxx"/>: 执行顺序为, filter1->原生filter子链->filter2, 同时去掉原生的xxxFilter
         */
    }

    /**
     * 判断group是否在groups中
     */
    private boolean isMatchGroup(String group, String[] groups) {
        if (StringUtils.isEmpty(group)) {
            return true;
        }
        if (groups != null && groups.length > 0) {
            for (String g : groups) {
                if (group.equals(g)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isActive(String[] keys, URL url) {
        // 如果@Activate注解中的value是空的直接返回true
        if (keys.length == 0) {
            return true;
        }
        // 遍历value
        for (String key : keys) {
            // 遍历URL参数
            for (Map.Entry<String, String> entry : url.getParameters().entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue();
                // 如果有一个参数同key一致，或者是以.key的方式结尾
                if ((k.equals(key) || k.endsWith("." + key)) && ConfigUtils.isNotEmpty(v)) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public T getLoadedExtension(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Extension name == null");
        }
        Holder<Object> holder = getOrCreateHolder(name);
        return (T) holder.get();
    }

    private Holder<Object> getOrCreateHolder(String name) {
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<>());
            holder = cachedInstances.get(name);
        }
        return holder;
    }

    public Set<String> getLoadedExtensions() {
        return Collections.unmodifiableSet(new TreeSet<>(cachedInstances.keySet()));
    }

    public Object getLoadedAdaptiveExtensionInstances() {
        return cachedAdaptiveInstance.get();
    }

    @SuppressWarnings("unchecked")
    public T getExtension(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Extension name == null");
        }
        if ("true".equals(name)) {
            // 获取默认的拓展实现类
            return getDefaultExtension();
        }
        // Holder，持有目标对象
        Holder<Object> holder = getOrCreateHolder(name);
        // 检查缓存
        Object instance = holder.get();
        // 双重检查
        if (instance == null) {
            synchronized (holder) {
                instance = holder.get();
                if (instance == null) {
                    // 缓存未命中则创建拓展对象
                    instance = createExtension(name);
                    // 设置实例到holder
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }

    public T getDefaultExtension() {
        getExtensionClasses();
        if (StringUtils.isBlank(cachedDefaultName) || "true".equals(cachedDefaultName)) {
            return null;
        }
        return getExtension(cachedDefaultName);
    }

    public boolean hasExtension(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Extension name == null");
        }
        Class<?> c = this.getExtensionClass(name);
        return c != null;
    }

    public Set<String> getSupportedExtensions() {
        Map<String, Class<?>> clazzes = getExtensionClasses();
        return Collections.unmodifiableSet(new TreeSet<>(clazzes.keySet()));
    }

    public String getDefaultExtensionName() {
        getExtensionClasses();
        return cachedDefaultName;
    }

    public void addExtension(String name, Class<?> clazz) {
        getExtensionClasses();
        // 实现关系
        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Input type " + clazz + " doesn't implement the Extension " + type);
        }
        // 不能是接口
        if (clazz.isInterface()) {
            throw new IllegalStateException("Input type " + clazz + " can't be interface!");
        }
        // 类上没有@Adaptive注解
        if (!clazz.isAnnotationPresent(Adaptive.class)) {
            if (StringUtils.isBlank(name)) {
                throw new IllegalStateException("Extension name is blank (Extension " + type + ")!");
            }
            if (cachedClasses.get().containsKey(name)) {
                throw new IllegalStateException("Extension name " + name + " already exists (Extension " + type + ")!");
            }
            cachedNames.put(clazz, name);
            cachedClasses.get().put(name, clazz);
        }
        // 类上有@Adaptive注解
        else {
            if (cachedAdaptiveClass != null) {
                throw new IllegalStateException("Adaptive Extension already exists (Extension " + type + ")!");
            }
            cachedAdaptiveClass = clazz;
        }
    }

    @Deprecated
    public void replaceExtension(String name, Class<?> clazz) {
        getExtensionClasses();
        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Input type " + clazz + " doesn't implement Extension " + type);
        }
        if (clazz.isInterface()) {
            throw new IllegalStateException("Input type " + clazz + " can't be interface!");
        }
        if (!clazz.isAnnotationPresent(Adaptive.class)) {
            if (StringUtils.isBlank(name)) {
                throw new IllegalStateException("Extension name is blank (Extension " + type + ")!");
            }
            if (!cachedClasses.get().containsKey(name)) {
                throw new IllegalStateException("Extension name " + name + " doesn't exist (Extension " + type + ")!");
            }
            cachedNames.put(clazz, name);
            cachedClasses.get().put(name, clazz);
            cachedInstances.remove(name);
        } else {
            if (cachedAdaptiveClass == null) {
                throw new IllegalStateException("Adaptive Extension doesn't exist (Extension " + type + ")!");
            }
            cachedAdaptiveClass = clazz;
            cachedAdaptiveInstance.set(null);
        }
    }

    /**
     * 获取自适应拓展
     */
    @SuppressWarnings("unchecked")
    public T getAdaptiveExtension() {
        // 从缓存中获取自适应拓展
        Object instance = cachedAdaptiveInstance.get();
        if (instance == null) {
            if (createAdaptiveInstanceError == null) {
                synchronized (cachedAdaptiveInstance) {
                    instance = cachedAdaptiveInstance.get();
                    if (instance == null) {
                        try {
                            // 创建自适应拓展
                            instance = createAdaptiveExtension();
                            // 设置自适应拓展到缓存中
                            cachedAdaptiveInstance.set(instance);
                        } catch (Throwable t) {
                            createAdaptiveInstanceError = t;
                            throw new IllegalStateException("Failed to create adaptive instance: " + t.toString(), t);
                        }
                    }
                }
            } else {
                throw new IllegalStateException("Failed to create adaptive instance: " + createAdaptiveInstanceError.toString(), createAdaptiveInstanceError);
            }
        }
        return (T) instance;
    }

    private IllegalStateException findException(String name) {
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (entry.getKey().toLowerCase().contains(name.toLowerCase())) {
                return entry.getValue();
            }
        }
        StringBuilder buf = new StringBuilder("No such extension " + type.getName() + " by name " + name);
        int i = 1;
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (i == 1) {
                buf.append(", possible causes: ");
            }
            buf.append("\r\n(");
            buf.append(i++);
            buf.append(") ");
            buf.append(entry.getKey());
            buf.append(":\r\n");
            buf.append(StringUtils.toString(entry.getValue()));
        }
        return new IllegalStateException(buf.toString());
    }

    /**
     * 创建拓展对象
     */
    @SuppressWarnings("unchecked")
    private T createExtension(String name) {
        // 获取所有的拓展类（实现类没有@Adaptive && 非Wrapper）
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw findException(name);
        }
        try {
            T instance = (T) EXTENSION_INSTANCES.get(clazz);
            if (instance == null) {
                // 反射创建拓展实例
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            }
            // 向拓展实例中注入依赖
            injectExtension(instance);
            Set<Class<?>> wrapperClasses = cachedWrapperClasses;
            // 将拓展对象包裹在相应的Wrapper对象
            if (CollectionUtils.isNotEmpty(wrapperClasses)) {
                // 循环创建Wrapper实例
                for (Class<?> wrapperClass : wrapperClasses) {
                    // 将当前instance作为参数传给Wrapper的构造方法，并通过反射创建Wrapper实例
                    // 然后向Wrapper实例中注入依赖，最后将Wrapper实例再次赋值给instance变量
                    // 这里包装ProtocolFilterWrapper...
                    instance = injectExtension((T) wrapperClass.getConstructor(type).newInstance(instance));
                }
            }
            return instance;
        } catch (Throwable t) {
            throw new IllegalStateException("Extension instance (name: " + name + ", class: " + type + ") couldn't be instantiated: " + t.getMessage(), t);
        }
    }

    /**
     * IOC
     * 通过setter方法注入依赖
     */
    private T injectExtension(T instance) {
        try {
            if (objectFactory != null) {
                // 遍历目标类的所有方法
                for (Method method : instance.getClass().getMethods()) {
                    // 检测方法是否以set开头，且方法仅有一个参数，且方法访问级别为public
                    if (isSetter(method)) {
                        if (method.getAnnotation(DisableInject.class) != null) {
                            continue;
                        }
                        // setter方法参数类型
                        Class<?> pt = method.getParameterTypes()[0];
                        if (ReflectUtils.isPrimitives(pt)) {
                            continue;
                        }
                        try {
                            // 属性名，比如setName方法对应属性名name
                            String property = getSetterProperty(method);
                            // 从ObjectFactory中获取依赖对象
                            Object object = objectFactory.getExtension(pt, property);
                            if (object != null) {
                                // 通过反射调用setter方法设置依赖
                                method.invoke(instance, object);
                            }
                        } catch (Exception e) {
                            logger.error("Failed to inject via method " + method.getName() + " of interface " + type.getName() + ": " + e.getMessage(), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return instance;
    }

    private String getSetterProperty(Method method) {
        return method.getName().length() > 3 ? method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4) : "";
    }

    private boolean isSetter(Method method) {
        return method.getName().startsWith("set") && method.getParameterTypes().length == 1 && Modifier.isPublic(method.getModifiers());
    }

    private Class<?> getExtensionClass(String name) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type == null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Extension name == null");
        }
        return getExtensionClasses().get(name);
    }

    /**
     * 获取所有的拓展类
     */
    private Map<String, Class<?>> getExtensionClasses() {
        // 从缓存中获取已加载的拓展类
        Map<String, Class<?>> classes = cachedClasses.get();
        // 双重检查
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    // 加载拓展类
                    classes = loadExtensionClasses();
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    /**
     * 加载拓展类
     * 对SPI注解进行解析
     * 调用loadDirectory方法加载指定文件夹配置文件
     */
    private Map<String, Class<?>> loadExtensionClasses() {
        // @SPI注解的值
        cacheDefaultExtensionName();
        Map<String, Class<?>> extensionClasses = new HashMap<>();
        /*
         * 加载指定文件夹下的配置文件
         * META-INF/dubbo/internal/
         * META-INF/dubbo/
         * META-INF/services/
         */
        loadDirectory(extensionClasses, DUBBO_INTERNAL_DIRECTORY, type.getName());
        // 兼容
        loadDirectory(extensionClasses, DUBBO_INTERNAL_DIRECTORY, type.getName().replace("org.apache", "com.alibaba"));
        loadDirectory(extensionClasses, DUBBO_DIRECTORY, type.getName());
        // 兼容
        loadDirectory(extensionClasses, DUBBO_DIRECTORY, type.getName().replace("org.apache", "com.alibaba"));
        loadDirectory(extensionClasses, SERVICES_DIRECTORY, type.getName());
        // 兼容
        loadDirectory(extensionClasses, SERVICES_DIRECTORY, type.getName().replace("org.apache", "com.alibaba"));
        return extensionClasses;
    }

    private void cacheDefaultExtensionName() {
        // 获取@SPI注解，这里的type变量是在调用getExtensionLoader方法时传入的
        final SPI defaultAnnotation = type.getAnnotation(SPI.class);
        if (defaultAnnotation != null) {
            String value = defaultAnnotation.value();
            if ((value = value.trim()).length() > 0) {
                // 对@SPI注解内容进行切分
                String[] names = NAME_SEPARATOR.split(value);
                // @SPI指定的默认实现只能有一个
                if (names.length > 1) {
                    throw new IllegalStateException("More than 1 default extension name on extension " + type.getName() + ": " + Arrays.toString(names));
                }
                // 设置默认名称，参考getDefaultExtension方法
                if (names.length == 1) {
                    cachedDefaultName = names[0];
                }
            }
        }
    }

    private void loadDirectory(Map<String, Class<?>> extensionClasses, String dir, String type) {
        // fileName = 文件夹路径 + type 全限定名
        String fileName = dir + type;
        try {
            Enumeration<java.net.URL> urls;
            ClassLoader classLoader = findClassLoader();
            // 根据文件名加载所有的同名文件
            if (classLoader != null) {
                urls = classLoader.getResources(fileName);
            } else {
                urls = ClassLoader.getSystemResources(fileName);
            }
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    java.net.URL resourceURL = urls.nextElement();
                    // 加载资源
                    loadResource(extensionClasses, classLoader, resourceURL);
                }
            }
        } catch (Throwable t) {
            logger.error("Exception occurred when loading extension class (interface: " + type + ", description file: " + fileName + ").", t);
        }
    }

    /**
     * 读取和解析配置文件，反射加载类，loadClass
     */
    private void loadResource(Map<String, Class<?>> extensionClasses, ClassLoader classLoader, java.net.URL resourceURL) {
        try {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceURL.openStream(), StandardCharsets.UTF_8))) {
                String line;
                // 按行读取配置内容
                while ((line = reader.readLine()) != null) {
                    // 定位#字符
                    final int ci = line.indexOf('#');
                    if (ci >= 0) {
                        // 截取#之前的字符串，#之后的内容为注释，忽略
                        line = line.substring(0, ci);
                    }
                    line = line.trim();
                    if (line.length() > 0) {
                        try {
                            String name = null;
                            int i = line.indexOf('=');
                            if (i > 0) {
                                // 以等于号=为界，截取键与值
                                name = line.substring(0, i).trim();
                                line = line.substring(i + 1).trim();
                            }
                            if (line.length() > 0) {
                                // 加载类，并通过loadClass方法对类进行缓存
                                loadClass(extensionClasses, resourceURL, Class.forName(line, true, classLoader), name);
                            }
                        } catch (Throwable t) {
                            IllegalStateException e = new IllegalStateException("Failed to load extension class (interface: " + type + ", class line: " + line + ") in " + resourceURL + ", cause: " + t.getMessage(), t);
                            exceptions.put(line, e);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            logger.error("Exception occurred when loading extension class (interface: " + type + ", class file: " + resourceURL + ") in " + resourceURL, t);
        }
    }

    private void loadClass(Map<String, Class<?>> extensionClasses, java.net.URL resourceURL, Class<?> clazz, String name) throws NoSuchMethodException {
        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Error occurred when loading extension class (interface: " + type + ", class line: " + clazz.getName() + "), class " + clazz.getName() + " is not subtype of interface.");
        }
        // clazz上是否有@Adaptive注解
        if (clazz.isAnnotationPresent(Adaptive.class)) {
            // cachedAdaptiveClass
            cacheAdaptiveClass(clazz);
        }
        // clazz是否是Wrapper类型（有clazz类型为参数的构造函数），比如ProtocolListenerWrapper、ProtocolFilterWrapper
        else if (isWrapperClass(clazz)) {
            // cachedWrapperClasses
            cacheWrapperClass(clazz);
        }
        // 进入此分支，表明clazz是普通的拓展类
        else {
            // clazz是否有默认的构造方法
            // 如果有含参构造但是没重写无参构造，会抛出java.lang.NoSuchMethodException
            clazz.getConstructor();
            if (StringUtils.isEmpty(name)) {
                // 如果name为空，则尝试从@Extension注解中获取name，或使用小写的类名作为name
                name = findAnnotationName(clazz);
                if (name.length() == 0) {
                    throw new IllegalStateException("No such extension name for the class " + clazz.getName() + " in the config " + resourceURL);
                }
            }
            // 切分name
            String[] names = NAME_SEPARATOR.split(name);
            if (ArrayUtils.isNotEmpty(names)) {
                // cachedActivates
                cacheActivateClass(clazz, names[0]);
                for (String n : names) {
                    // cachedNames
                    cacheName(clazz, n);
                    // extensionClasses
                    saveInExtensionClass(extensionClasses, clazz, name);
                }
            }
        }
    }

    private void cacheName(Class<?> clazz, String name) {
        if (!cachedNames.containsKey(clazz)) {
            cachedNames.put(clazz, name);
        }
    }

    private void saveInExtensionClass(Map<String, Class<?>> extensionClasses, Class<?> clazz, String name) {
        Class<?> c = extensionClasses.get(name);
        if (c == null) {
            // 存储名称到Class的映射关系
            extensionClasses.put(name, clazz);
        } else if (c != clazz) {
            throw new IllegalStateException("Duplicate extension " + type.getName() + " name " + name + " on " + c.getName() + " and " + clazz.getName());
        }
    }

    private void cacheActivateClass(Class<?> clazz, String name) {
        Activate activate = clazz.getAnnotation(Activate.class);
        // 如果类上有@Activate注解
        if (activate != null) {
            // 存储name到@Activate注解对象的映射关系
            cachedActivates.put(name, activate);
        }
    }

    private void cacheAdaptiveClass(Class<?> clazz) {
        if (cachedAdaptiveClass == null) {
            cachedAdaptiveClass = clazz;
        } else if (!cachedAdaptiveClass.equals(clazz)) {
            throw new IllegalStateException("More than 1 adaptive class found: " + cachedAdaptiveClass.getClass().getName() + ", " + clazz.getClass().getName());
        }
    }

    private void cacheWrapperClass(Class<?> clazz) {
        if (cachedWrapperClasses == null) {
            cachedWrapperClasses = new ConcurrentHashSet<>();
        }
        cachedWrapperClasses.add(clazz);
    }

    private boolean isWrapperClass(Class<?> clazz) {
        try {
            clazz.getConstructor(type);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    private String findAnnotationName(Class<?> clazz) {
        org.apache.dubbo.common.Extension extension = clazz.getAnnotation(org.apache.dubbo.common.Extension.class);
        if (extension == null) {
            String name = clazz.getSimpleName();
            if (name.endsWith(type.getSimpleName())) {
                name = name.substring(0, name.length() - type.getSimpleName().length());
            }
            return name.toLowerCase();
        }
        return extension.value();
    }

    @SuppressWarnings("unchecked")
    private T createAdaptiveExtension() {
        try {
            // 获取自适应拓展类，并通过反射实例化，调用injectExtension向手工编码的拓展实例中注入依赖
            // Dubbo中有两种类型的自适应拓展，一种是手工编码的，可能存在依赖；一种是自动生成的，不依赖其他类
            return injectExtension((T) getAdaptiveExtensionClass().newInstance());
        } catch (Exception e) {
            throw new IllegalStateException("Can't create adaptive extension " + type + ", cause: " + e.getMessage(), e);
        }
    }

    private Class<?> getAdaptiveExtensionClass() {
        // 通过SPI获取所有的拓展类，如果某个实现类被@Adaptive注解修饰了，该类会被赋值给cachedAdaptiveClass变量
        getExtensionClasses();
        // 检查缓存，若缓存不为空，则直接返回缓存
        if (cachedAdaptiveClass != null) {
            return cachedAdaptiveClass;
        }
        // 如果所有的实现类均未被@Adaptive注解修饰，自己生成，创建自适应拓展类（但是方法上必须有@Adaptive注解）
        return cachedAdaptiveClass = createAdaptiveExtensionClass();
    }

    /**
     * 生成自适应拓展类
     */
    private Class<?> createAdaptiveExtensionClass() {
        // 构建Adaptive自适应拓展代码
        String code = new AdaptiveClassCodeGenerator(type, cachedDefaultName).generate();
        ClassLoader classLoader = findClassLoader();
        // 获取编译器实现类，默认javassist
        org.apache.dubbo.common.compiler.Compiler compiler = ExtensionLoader.getExtensionLoader(org.apache.dubbo.common.compiler.Compiler.class).getAdaptiveExtension();
        // 编译代码，生成Class
        return compiler.compile(code, classLoader);
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "[" + type.getName() + "]";
    }

}
