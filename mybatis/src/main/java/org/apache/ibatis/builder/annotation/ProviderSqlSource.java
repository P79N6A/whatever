package org.apache.ibatis.builder.annotation;

import org.apache.ibatis.annotations.Lang;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * 当SQL语句通过指定的类和方法获取时(使用@XXXProvider注解)，通过反射调用相应的方法得到SQL语句
 */
public class ProviderSqlSource implements SqlSource {

    private final Configuration configuration;
    private final Class<?> providerType;
    private final LanguageDriver languageDriver;
    private Method providerMethod;
    private String[] providerMethodArgumentNames;
    private Class<?>[] providerMethodParameterTypes;
    private ProviderContext providerContext;
    private Integer providerContextIndex;

    @Deprecated
    public ProviderSqlSource(Configuration configuration, Object provider) {
        this(configuration, provider, null, null);
    }

    public ProviderSqlSource(Configuration configuration, Object provider, Class<?> mapperType, Method mapperMethod) {
        String providerMethodName;
        try {
            this.configuration = configuration;
            Lang lang = mapperMethod == null ? null : mapperMethod.getAnnotation(Lang.class);
            this.languageDriver = configuration.getLanguageDriver(lang == null ? null : lang.value());
            // provider类对象
            this.providerType = (Class<?>) provider.getClass().getMethod("type").invoke(provider);
            // provider类对象生成sql的方法名
            providerMethodName = (String) provider.getClass().getMethod("method").invoke(provider);

            // 没有注明method && provider类对象继承ProviderMethodResolver
            if (providerMethodName.length() == 0 && ProviderMethodResolver.class.isAssignableFrom(this.providerType)) {
                this.providerMethod = ((ProviderMethodResolver) this.providerType.getDeclaredConstructor().newInstance()).resolveMethod(new ProviderContext(mapperType, mapperMethod, configuration.getDatabaseId()));
            }
            if (this.providerMethod == null) {
                providerMethodName = providerMethodName.length() == 0 ? "provideSql" : providerMethodName;
                for (Method m : this.providerType.getMethods()) {
                    if (providerMethodName.equals(m.getName()) && CharSequence.class.isAssignableFrom(m.getReturnType())) {
                        if (this.providerMethod != null) {
                            throw new BuilderException("Error creating SqlSource for SqlProvider. Method '" + providerMethodName + "' is found multiple in SqlProvider '" + this.providerType.getName() + "'. Sql provider method can not overload.");
                        }
                        // provider类对象生成sql的方法
                        this.providerMethod = m;
                    }
                }
            }
        } catch (BuilderException e) {
            throw e;
        } catch (Exception e) {
            throw new BuilderException("Error creating SqlSource for SqlProvider.  Cause: " + e, e);
        }
        if (this.providerMethod == null) {
            throw new BuilderException("Error creating SqlSource for SqlProvider. Method '" + providerMethodName + "' not found in SqlProvider '" + this.providerType.getName() + "'.");
        }
        // provider类对象生成sql的方法的参数名数组
        this.providerMethodArgumentNames = new ParamNameResolver(configuration, this.providerMethod).getNames();
        // provider类对象生成sql的方法的参数类型
        this.providerMethodParameterTypes = this.providerMethod.getParameterTypes();
        for (int i = 0; i < this.providerMethodParameterTypes.length; i++) {
            Class<?> parameterType = this.providerMethodParameterTypes[i];
            if (parameterType == ProviderContext.class) {
                if (this.providerContext != null) {
                    throw new BuilderException("Error creating SqlSource for SqlProvider. ProviderContext found multiple in SqlProvider method (" + this.providerType.getName() + "." + providerMethod.getName() + "). ProviderContext can not define multiple in SqlProvider method argument.");
                }
                this.providerContext = new ProviderContext(mapperType, mapperMethod, configuration.getDatabaseId());
                this.providerContextIndex = i;
            }
        }
    }

    @Override
    public BoundSql getBoundSql(Object parameterObject) {
        SqlSource sqlSource = createSqlSource(parameterObject);
        return sqlSource.getBoundSql(parameterObject);
    }

    private SqlSource createSqlSource(Object parameterObject) {
        try {
            int bindParameterCount = providerMethodParameterTypes.length - (providerContext == null ? 0 : 1);
            String sql;
            if (providerMethodParameterTypes.length == 0) {
                sql = invokeProviderMethod();
            } else if (bindParameterCount == 0) {
                sql = invokeProviderMethod(providerContext);
            } else if (bindParameterCount == 1 && (parameterObject == null || providerMethodParameterTypes[providerContextIndex == null || providerContextIndex == 1 ? 0 : 1].isAssignableFrom(parameterObject.getClass()))) {
                sql = invokeProviderMethod(extractProviderMethodArguments(parameterObject));
            } else if (parameterObject instanceof Map) {
                @SuppressWarnings("unchecked") Map<String, Object> params = (Map<String, Object>) parameterObject;
                sql = invokeProviderMethod(extractProviderMethodArguments(params, providerMethodArgumentNames));
            } else {
                throw new BuilderException("Error invoking SqlProvider method (" + providerType.getName() + "." + providerMethod.getName() + "). Cannot invoke a method that holds " + (bindParameterCount == 1 ? "named argument(@Param)" : "multiple arguments") + " using a specifying parameterObject. In this case, please specify a 'java.util.Map' object.");
            }
            Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
            return languageDriver.createSqlSource(configuration, sql, parameterType);
        } catch (BuilderException e) {
            throw e;
        } catch (Exception e) {
            throw new BuilderException("Error invoking SqlProvider method (" + providerType.getName() + "." + providerMethod.getName() + ").  Cause: " + e, e);
        }
    }

    private Object[] extractProviderMethodArguments(Object parameterObject) {
        if (providerContext != null) {
            Object[] args = new Object[2];
            args[providerContextIndex == 0 ? 1 : 0] = parameterObject;
            args[providerContextIndex] = providerContext;
            return args;
        } else {
            return new Object[]{parameterObject};
        }
    }

    private Object[] extractProviderMethodArguments(Map<String, Object> params, String[] argumentNames) {
        Object[] args = new Object[argumentNames.length];
        for (int i = 0; i < args.length; i++) {
            if (providerContextIndex != null && providerContextIndex == i) {
                args[i] = providerContext;
            } else {
                args[i] = params.get(argumentNames[i]);
            }
        }
        return args;
    }

    private String invokeProviderMethod(Object... args) throws Exception {
        Object targetObject = null;
        if (!Modifier.isStatic(providerMethod.getModifiers())) {
            targetObject = providerType.newInstance();
        }
        CharSequence sql = (CharSequence) providerMethod.invoke(targetObject, args);
        return sql != null ? sql.toString() : null;
    }

}
