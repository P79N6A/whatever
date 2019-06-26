package org.apache.ibatis.mapping;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.session.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SqlSource中包含的SQL，以及绑定的参数
 */
public class BoundSql {

    /**
     * 处理动态内容之后的实际SQL语句，包含?占位符，即最终给JDBC的SQL语句
     */
    private final String sql;

    /**
     * Java - Jdbc
     */
    private final List<ParameterMapping> parameterMappings;

    /**
     * 具体参数
     */
    private final Object parameterObject;

    /**
     * 额外参数，也就是for、bind生成的
     */
    private final Map<String, Object> additionalParameters;

    /**
     * 额外参数的facade模式包装
     */
    private final MetaObject metaParameters;

    public BoundSql(Configuration configuration, String sql, List<ParameterMapping> parameterMappings, Object parameterObject) {
        this.sql = sql;
        this.parameterMappings = parameterMappings;
        this.parameterObject = parameterObject;
        this.additionalParameters = new HashMap<>();
        this.metaParameters = configuration.newMetaObject(additionalParameters);
    }

    public String getSql() {
        return sql;
    }

    public List<ParameterMapping> getParameterMappings() {
        return parameterMappings;
    }

    public Object getParameterObject() {
        return parameterObject;
    }

    public boolean hasAdditionalParameter(String name) {
        String paramName = new PropertyTokenizer(name).getName();
        return additionalParameters.containsKey(paramName);
    }

    public void setAdditionalParameter(String name, Object value) {
        metaParameters.setValue(name, value);
    }

    public Object getAdditionalParameter(String name) {
        return metaParameters.getValue(name);
    }
}
