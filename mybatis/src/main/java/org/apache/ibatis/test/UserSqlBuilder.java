package org.apache.ibatis.test;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.jdbc.SQL;

public class UserSqlBuilder {

    // If not use @Param, you should be define same arguments with mapper method
    public static String buildGetUsersByName(final String name, final String orderByColumn) {
        return new SQL() {{
            SELECT("*");
            FROM("users");
            WHERE("name like #{name} || '%'");
            ORDER_BY(orderByColumn);
        }}.toString();
    }

    // If use @Param, you can define only arguments to be used
    public static String buildGetUsersByName(@Param("orderByColumn") final String orderByColumn) {
        return new SQL() {{
            SELECT("*");
            FROM("users");
            WHERE("name like #{name} || '%'");
            ORDER_BY(orderByColumn);
        }}.toString();
    }
}