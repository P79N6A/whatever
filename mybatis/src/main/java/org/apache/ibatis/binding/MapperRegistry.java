package org.apache.ibatis.binding;

import org.apache.ibatis.builder.annotation.MapperAnnotationBuilder;
import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;

import java.util.*;

public class MapperRegistry {

    private final Configuration config;

    /**
     * Mapper接口和代理类是MapperProxyFactory的映射关系
     */
    private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new HashMap<>();

    public MapperRegistry(Configuration config) {
        this.config = config;
    }

    @SuppressWarnings("unchecked")
    public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
        final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
        if (mapperProxyFactory == null) {
            throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
        }
        try {
            return mapperProxyFactory.newInstance(sqlSession);
        } catch (Exception e) {
            throw new BindingException("Error getting mapper instance. Cause: " + e, e);
        }
    }

    public <T> boolean hasMapper(Class<T> type) {
        return knownMappers.containsKey(type);
    }

    public <T> void addMapper(Class<T> type) {
        // 必须是interface
        if (type.isInterface()) {
            // 确保只会加载一次不会被覆盖
            if (hasMapper(type)) {
                throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
            }
            boolean loadCompleted = false;
            try {
                // 为Mapper接口创建一个MapperProxyFactory代理
                knownMappers.put(type, new MapperProxyFactory<>(type));

                MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
                // 具体的Mapper接口文件加载与解析
                parser.parse();
                loadCompleted = true;
            } finally {
                if (!loadCompleted) {
                    // 剔移除解析异常的接口
                    knownMappers.remove(type);
                }
            }
        }
    }

    public Collection<Class<?>> getMappers() {
        return Collections.unmodifiableCollection(knownMappers.keySet());
    }

    public void addMappers(String packageName, Class<?> superType) {
        ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<>();
        // 搜索classpath下package及子package中者继承于某个类/接口的类
        // 加载所有的类，因为传递了Object.class作为父类
        resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
        // 所有匹配的class都存在ResolverUtil.matches中
        Set<Class<? extends Class<?>>> mapperSet = resolverUtil.getClasses();
        for (Class<?> mapperClass : mapperSet) {
            // 具体的mapper类/接口解析
            addMapper(mapperClass);
        }
    }

    public void addMappers(String packageName) {
        addMappers(packageName, Object.class);
    }

}
