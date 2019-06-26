package org.apache.ibatis.executor.resultset;

import org.apache.ibatis.cursor.Cursor;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * 结果集处理器，将JDBC查询结果映射到Java对象
 */
public interface ResultSetHandler {

    /**
     * 处理Statement执行后产生的结果集，生成结果列表
     */
    <E> List<E> handleResultSets(Statement stmt) throws SQLException;

    <E> Cursor<E> handleCursorResultSets(Statement stmt) throws SQLException;

    /**
     * 处理存储过程执行后的输出参数
     */
    void handleOutputParameters(CallableStatement cs) throws SQLException;

}
