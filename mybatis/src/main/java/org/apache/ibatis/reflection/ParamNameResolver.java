package org.apache.ibatis.reflection;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class ParamNameResolver {

    private static final String GENERIC_NAME_PREFIX = "param";

    /**
     * 方法注解位置和具体值
     */
    private final SortedMap<Integer, String> names;

    private boolean hasParamAnnotation;

    public ParamNameResolver(Configuration config, Method method) {
        final Class<?>[] paramTypes = method.getParameterTypes();
        final Annotation[][] paramAnnotations = method.getParameterAnnotations();
        final SortedMap<Integer, String> map = new TreeMap<>();
        // 方法参数上的注解数量
        int paramCount = paramAnnotations.length;

        for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
            // RowBounds ResultHandler 跳过
            if (isSpecialParameter(paramTypes[paramIndex])) {

                continue;
            }
            String name = null;
            for (Annotation annotation : paramAnnotations[paramIndex]) {
                if (annotation instanceof Param) {
                    hasParamAnnotation = true;
                    // @Param注解值
                    name = ((Param) annotation).value();
                    break;
                }
            }
            if (name == null) {

                // 使用方法签名中的名称作为语句参数名称
                if (config.isUseActualParamName()) {
                    // 参数的名称
                    name = getActualParamName(method, paramIndex);
                }
                if (name == null) {

                    name = String.valueOf(map.size());
                }
            }
            map.put(paramIndex, name);
        }
        names = Collections.unmodifiableSortedMap(map);
    }

    private String getActualParamName(Method method, int paramIndex) {
        return ParamNameUtil.getParamNames(method).get(paramIndex);
    }

    private static boolean isSpecialParameter(Class<?> clazz) {
        return RowBounds.class.isAssignableFrom(clazz) || ResultHandler.class.isAssignableFrom(clazz);
    }

    public String[] getNames() {
        return names.values().toArray(new String[0]);
    }

    public Object getNamedParams(Object[] args) {
        final int paramCount = names.size();
        // 没有参数
        if (args == null || paramCount == 0) {
            return null;
        }
        // 没有@Param && 只有一个参数
        else if (!hasParamAnnotation && paramCount == 1) {
            return args[names.firstKey()];
        }
        //
        else {
            final Map<String, Object> param = new ParamMap<>();
            int i = 0;
            for (Map.Entry<Integer, String> entry : names.entrySet()) {
                // 参数名 参数值
                param.put(entry.getValue(), args[entry.getKey()]);

                // param1 param2 etc.
                final String genericParamName = GENERIC_NAME_PREFIX + String.valueOf(i + 1);

                if (!names.containsValue(genericParamName)) {
                    // paramX 参数值
                    param.put(genericParamName, args[entry.getKey()]);
                }
                i++;
            }
            return param;
        }
    }
}
