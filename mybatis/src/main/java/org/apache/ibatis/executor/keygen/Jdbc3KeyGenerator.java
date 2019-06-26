package org.apache.ibatis.executor.keygen;

import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.ArrayUtil;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.defaults.DefaultSqlSession.StrictMap;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.Map.Entry;

/**
 * insert是否取回表的自增Id
 */
public class Jdbc3KeyGenerator implements KeyGenerator {

    public static final Jdbc3KeyGenerator INSTANCE = new Jdbc3KeyGenerator();

    private static final String MSG_TOO_MANY_KEYS = "Too many keys are generated. There are only %d target objects. " + "You either specified a wrong 'keyProperty' or encountered a driver bug like #1523.";

    @Override
    public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
        // 执行前处理，对应mapper里面的selectKey中的order="BEFORE"属性，先执行查询key，并设置到参数对象中
        // Jdbc只能是执行之后返回主键id
    }

    @Override
    public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
        processBatch(ms, stmt, parameter);
    }

    public void processBatch(MappedStatement ms, Statement stmt, Object parameter) {
        final String[] keyProperties = ms.getKeyProperties();
        if (keyProperties == null || keyProperties.length == 0) {
            return;
        }
        // 调用java.sql.Statement.getGeneratedKeys返回insert语句执行后生成的自增长主键
        try (ResultSet rs = stmt.getGeneratedKeys()) {
            final ResultSetMetaData rsmd = rs.getMetaData();
            final Configuration configuration = ms.getConfiguration();
            if (rsmd.getColumnCount() < keyProperties.length) {

            } else {
                // 将取回的id回写到传入的parameter对象
                assignKeys(configuration, rs, rsmd, keyProperties, parameter);
            }
        } catch (Exception e) {
            throw new ExecutorException("Error getting generated key or setting result to parameter object. Cause: " + e, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void assignKeys(Configuration configuration, ResultSet rs, ResultSetMetaData rsmd, String[] keyProperties, Object parameter) throws SQLException {
        if (parameter instanceof ParamMap || parameter instanceof StrictMap) {

            assignKeysToParamMap(configuration, rs, rsmd, keyProperties, (Map<String, ?>) parameter);
        } else if (parameter instanceof ArrayList && !((ArrayList<?>) parameter).isEmpty() && ((ArrayList<?>) parameter).get(0) instanceof ParamMap) {

            assignKeysToParamMapList(configuration, rs, rsmd, keyProperties, ((ArrayList<ParamMap<?>>) parameter));
        } else {

            assignKeysToParam(configuration, rs, rsmd, keyProperties, parameter);
        }
    }

    private void assignKeysToParam(Configuration configuration, ResultSet rs, ResultSetMetaData rsmd, String[] keyProperties, Object parameter) throws SQLException {
        Collection<?> params = collectionize(parameter);
        if (params.isEmpty()) {
            return;
        }
        List<KeyAssigner> assignerList = new ArrayList<>();
        for (int i = 0; i < keyProperties.length; i++) {
            assignerList.add(new KeyAssigner(configuration, rsmd, i + 1, null, keyProperties[i]));
        }
        Iterator<?> iterator = params.iterator();
        while (rs.next()) {
            if (!iterator.hasNext()) {
                throw new ExecutorException(String.format(MSG_TOO_MANY_KEYS, params.size()));
            }
            Object param = iterator.next();
            assignerList.forEach(x -> x.assign(rs, param));
        }
    }

    private void assignKeysToParamMapList(Configuration configuration, ResultSet rs, ResultSetMetaData rsmd, String[] keyProperties, ArrayList<ParamMap<?>> paramMapList) throws SQLException {
        Iterator<ParamMap<?>> iterator = paramMapList.iterator();
        List<KeyAssigner> assignerList = new ArrayList<>();
        long counter = 0;
        while (rs.next()) {
            if (!iterator.hasNext()) {
                throw new ExecutorException(String.format(MSG_TOO_MANY_KEYS, counter));
            }
            ParamMap<?> paramMap = iterator.next();
            if (assignerList.isEmpty()) {
                for (int i = 0; i < keyProperties.length; i++) {
                    assignerList.add(getAssignerForParamMap(configuration, rsmd, i + 1, paramMap, keyProperties[i], keyProperties, false).getValue());
                }
            }
            assignerList.forEach(x -> x.assign(rs, paramMap));
            counter++;
        }
    }

    private void assignKeysToParamMap(Configuration configuration, ResultSet rs, ResultSetMetaData rsmd, String[] keyProperties, Map<String, ?> paramMap) throws SQLException {
        if (paramMap.isEmpty()) {
            return;
        }
        Map<String, Entry<Iterator<?>, List<KeyAssigner>>> assignerMap = new HashMap<>();
        for (int i = 0; i < keyProperties.length; i++) {
            Entry<String, KeyAssigner> entry = getAssignerForParamMap(configuration, rsmd, i + 1, paramMap, keyProperties[i], keyProperties, true);
            Entry<Iterator<?>, List<KeyAssigner>> iteratorPair = assignerMap.computeIfAbsent(entry.getKey(), k -> entry(collectionize(paramMap.get(k)).iterator(), new ArrayList<>()));
            iteratorPair.getValue().add(entry.getValue());
        }
        long counter = 0;
        while (rs.next()) {
            for (Entry<Iterator<?>, List<KeyAssigner>> pair : assignerMap.values()) {
                if (!pair.getKey().hasNext()) {
                    throw new ExecutorException(String.format(MSG_TOO_MANY_KEYS, counter));
                }
                Object param = pair.getKey().next();
                pair.getValue().forEach(x -> x.assign(rs, param));
            }
            counter++;
        }
    }

    private Entry<String, KeyAssigner> getAssignerForParamMap(Configuration config, ResultSetMetaData rsmd, int columnPosition, Map<String, ?> paramMap, String keyProperty, String[] keyProperties, boolean omitParamName) {
        boolean singleParam = paramMap.values().stream().distinct().count() == 1;
        int firstDot = keyProperty.indexOf('.');
        if (firstDot == -1) {
            if (singleParam) {
                return getAssignerForSingleParam(config, rsmd, columnPosition, paramMap, keyProperty, omitParamName);
            }
            throw new ExecutorException("Could not determine which parameter to assign generated keys to. " + "Note that when there are multiple parameters, 'keyProperty' must include the parameter name (e.g. 'param.id'). " + "Specified key properties are " + ArrayUtil.toString(keyProperties) + " and available parameters are " + paramMap.keySet());
        }
        String paramName = keyProperty.substring(0, firstDot);
        if (paramMap.containsKey(paramName)) {
            String argParamName = omitParamName ? null : paramName;
            String argKeyProperty = keyProperty.substring(firstDot + 1);
            return entry(paramName, new KeyAssigner(config, rsmd, columnPosition, argParamName, argKeyProperty));
        } else if (singleParam) {
            return getAssignerForSingleParam(config, rsmd, columnPosition, paramMap, keyProperty, omitParamName);
        } else {
            throw new ExecutorException("Could not find parameter '" + paramName + "'. " + "Note that when there are multiple parameters, 'keyProperty' must include the parameter name (e.g. 'param.id'). " + "Specified key properties are " + ArrayUtil.toString(keyProperties) + " and available parameters are " + paramMap.keySet());
        }
    }

    private Entry<String, KeyAssigner> getAssignerForSingleParam(Configuration config, ResultSetMetaData rsmd, int columnPosition, Map<String, ?> paramMap, String keyProperty, boolean omitParamName) {

        String singleParamName = nameOfSingleParam(paramMap);
        String argParamName = omitParamName ? null : singleParamName;
        return entry(singleParamName, new KeyAssigner(config, rsmd, columnPosition, argParamName, keyProperty));
    }

    private static String nameOfSingleParam(Map<String, ?> paramMap) {

        return paramMap.keySet().iterator().next();
    }

    private static Collection<?> collectionize(Object param) {
        if (param instanceof Collection) {
            return (Collection<?>) param;
        } else if (param instanceof Object[]) {
            return Arrays.asList((Object[]) param);
        } else {
            return Arrays.asList(param);
        }
    }

    private static <K, V> Entry<K, V> entry(K key, V value) {

        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    private class KeyAssigner {
        private final Configuration configuration;
        private final ResultSetMetaData rsmd;
        private final TypeHandlerRegistry typeHandlerRegistry;
        private final int columnPosition;
        private final String paramName;
        private final String propertyName;
        private TypeHandler<?> typeHandler;

        protected KeyAssigner(Configuration configuration, ResultSetMetaData rsmd, int columnPosition, String paramName, String propertyName) {
            super();
            this.configuration = configuration;
            this.rsmd = rsmd;
            this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            this.columnPosition = columnPosition;
            this.paramName = paramName;
            this.propertyName = propertyName;
        }

        protected void assign(ResultSet rs, Object param) {
            if (paramName != null) {

                param = ((ParamMap<?>) param).get(paramName);
            }
            MetaObject metaParam = configuration.newMetaObject(param);
            try {
                if (typeHandler == null) {
                    if (metaParam.hasSetter(propertyName)) {
                        Class<?> propertyType = metaParam.getSetterType(propertyName);
                        typeHandler = typeHandlerRegistry.getTypeHandler(propertyType, JdbcType.forCode(rsmd.getColumnType(columnPosition)));
                    } else {
                        throw new ExecutorException("No setter found for the keyProperty '" + propertyName + "' in '" + metaParam.getOriginalObject().getClass().getName() + "'.");
                    }
                }
                if (typeHandler == null) {

                } else {
                    Object value = typeHandler.getResult(rs, columnPosition);
                    metaParam.setValue(propertyName, value);
                }
            } catch (SQLException e) {
                throw new ExecutorException("Error getting generated key or setting result to parameter object. Cause: " + e, e);
            }
        }
    }
}
