package org.apache.ibatis.jdbc;

@Deprecated
public class SelectBuilder {

    private static final ThreadLocal<SQL> localSQL = new ThreadLocal<>();

    static {
        BEGIN();
    }

    private SelectBuilder() {

    }

    public static void BEGIN() {
        RESET();
    }

    public static void RESET() {
        localSQL.set(new SQL());
    }

    public static void SELECT(String columns) {
        sql().SELECT(columns);
    }

    public static void SELECT_DISTINCT(String columns) {
        sql().SELECT_DISTINCT(columns);
    }

    public static void FROM(String table) {
        sql().FROM(table);
    }

    public static void JOIN(String join) {
        sql().JOIN(join);
    }

    public static void INNER_JOIN(String join) {
        sql().INNER_JOIN(join);
    }

    public static void LEFT_OUTER_JOIN(String join) {
        sql().LEFT_OUTER_JOIN(join);
    }

    public static void RIGHT_OUTER_JOIN(String join) {
        sql().RIGHT_OUTER_JOIN(join);
    }

    public static void OUTER_JOIN(String join) {
        sql().OUTER_JOIN(join);
    }

    public static void WHERE(String conditions) {
        sql().WHERE(conditions);
    }

    public static void OR() {
        sql().OR();
    }

    public static void AND() {
        sql().AND();
    }

    public static void GROUP_BY(String columns) {
        sql().GROUP_BY(columns);
    }

    public static void HAVING(String conditions) {
        sql().HAVING(conditions);
    }

    public static void ORDER_BY(String columns) {
        sql().ORDER_BY(columns);
    }

    public static String SQL() {
        try {
            return sql().toString();
        } finally {
            RESET();
        }
    }

    private static SQL sql() {
        return localSQL.get();
    }

}
