package org.apache.dubbo.common.utils;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class CompatibleTypeUtils {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private CompatibleTypeUtils() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object compatibleTypeConvert(Object value, Class<?> type) {
        if (value == null || type == null || type.isAssignableFrom(value.getClass())) {
            return value;
        }
        if (value instanceof String) {
            String string = (String) value;
            if (char.class.equals(type) || Character.class.equals(type)) {
                if (string.length() != 1) {
                    throw new IllegalArgumentException(String.format("CAN NOT convert String(%s) to char!" + " when convert String to char, the String MUST only 1 char.", string));
                }
                return string.charAt(0);
            } else if (type.isEnum()) {
                return Enum.valueOf((Class<Enum>) type, string);
            } else if (type == BigInteger.class) {
                return new BigInteger(string);
            } else if (type == BigDecimal.class) {
                return new BigDecimal(string);
            } else if (type == Short.class || type == short.class) {
                return new Short(string);
            } else if (type == Integer.class || type == int.class) {
                return new Integer(string);
            } else if (type == Long.class || type == long.class) {
                return new Long(string);
            } else if (type == Double.class || type == double.class) {
                return new Double(string);
            } else if (type == Float.class || type == float.class) {
                return new Float(string);
            } else if (type == Byte.class || type == byte.class) {
                return new Byte(string);
            } else if (type == Boolean.class || type == boolean.class) {
                return new Boolean(string);
            } else if (type == Date.class || type == java.sql.Date.class || type == java.sql.Timestamp.class || type == java.sql.Time.class) {
                try {
                    Date date = new SimpleDateFormat(DATE_FORMAT).parse((String) value);
                    if (type == java.sql.Date.class) {
                        return new java.sql.Date(date.getTime());
                    } else if (type == java.sql.Timestamp.class) {
                        return new java.sql.Timestamp(date.getTime());
                    } else if (type == java.sql.Time.class) {
                        return new java.sql.Time(date.getTime());
                    } else {
                        return date;
                    }
                } catch (ParseException e) {
                    throw new IllegalStateException("Failed to parse date " + value + " by format " + DATE_FORMAT + ", cause: " + e.getMessage(), e);
                }
            } else if (type == Class.class) {
                try {
                    return ReflectUtils.name2class((String) value);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            } else if (char[].class.equals(type)) {
                int len = string.length();
                char[] chars = new char[len];
                string.getChars(0, len, chars, 0);
                return chars;
            }
        } else if (value instanceof Number) {
            Number number = (Number) value;
            if (type == byte.class || type == Byte.class) {
                return number.byteValue();
            } else if (type == short.class || type == Short.class) {
                return number.shortValue();
            } else if (type == int.class || type == Integer.class) {
                return number.intValue();
            } else if (type == long.class || type == Long.class) {
                return number.longValue();
            } else if (type == float.class || type == Float.class) {
                return number.floatValue();
            } else if (type == double.class || type == Double.class) {
                return number.doubleValue();
            } else if (type == BigInteger.class) {
                return BigInteger.valueOf(number.longValue());
            } else if (type == BigDecimal.class) {
                return BigDecimal.valueOf(number.doubleValue());
            } else if (type == Date.class) {
                return new Date(number.longValue());
            }
        } else if (value instanceof Collection) {
            Collection collection = (Collection) value;
            if (type.isArray()) {
                int length = collection.size();
                Object array = Array.newInstance(type.getComponentType(), length);
                int i = 0;
                for (Object item : collection) {
                    Array.set(array, i++, item);
                }
                return array;
            } else if (!type.isInterface()) {
                try {
                    Collection result = (Collection) type.newInstance();
                    result.addAll(collection);
                    return result;
                } catch (Throwable e) {
                }
            } else if (type == List.class) {
                return new ArrayList<Object>(collection);
            } else if (type == Set.class) {
                return new HashSet<Object>(collection);
            }
        } else if (value.getClass().isArray() && Collection.class.isAssignableFrom(type)) {
            Collection collection;
            if (!type.isInterface()) {
                try {
                    collection = (Collection) type.newInstance();
                } catch (Throwable e) {
                    collection = new ArrayList<Object>();
                }
            } else if (type == Set.class) {
                collection = new HashSet<Object>();
            } else {
                collection = new ArrayList<Object>();
            }
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                collection.add(Array.get(value, i));
            }
            return collection;
        }
        return value;
    }

}