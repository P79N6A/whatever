package org.apache.ibatis.executor.keygen;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;

import java.sql.Statement;

/**
 * insert语句默认是不返回记录的主键值，而是返回插入的记录条数
 * 不同数据库对主键的生成不一样
 */
public interface KeyGenerator {

    /**
     * 对应mapper的selectKey属性order="BEFORE"，先执行查询key，并设置到参数对象中
     * 对Sequence主键，insert前必须指定一个主键值给要插入的记录，如Oracle
     */
    void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter);

    /**
     * 对应mapper的selectKey属性order="AFTER"，执行后，再查一遍key，设置到参数对象中
     * 对自增主键，插入时不需要主键，而是在插入过程自动获取一个自增的主键，如MySQL
     */
    void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter);

}
