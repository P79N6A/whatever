package org.apache.ibatis.session;

import java.sql.Connection;

/**
 * 数据库的隔离级别有4个，由低到高依次为Read uncommitted、Read committed、Repeatable read、Serializable
 * 这四个级别可以逐个解决脏读、不可重复读、幻读这几类问题
 * 脏读：如果一个事务对数据进行了更新，但事务还没有提交，另一个事务就可以看到该事务没有提交的更新结果，如果第一个事务回滚，第二个事务之前看到的数据就是脏数据
 * 不可重复读：同个事务在整个事务过程中对同一笔数据进行读取，每次读取结果都不同，如果事务1在事务2的更新之前读取一次数据，在事务2的更新操作之后再读取同一笔数据，两次结果是不同的
 * 幻读：同样一个查询在整个事务过程中多次执行后，查询所得的结果集不一样，针对的是多笔记录
 */
public enum TransactionIsolationLevel {

    /**
     *
     */
    NONE(Connection.TRANSACTION_NONE),

    /**
     * Read committed
     * 读提交，可防止脏读，Oracle等多数数据库默认
     * 执行了COMMIT后，别的事务就能读到这个改变，只能读取到已经提交的数据
     * 脏读×
     * 不可重复读√
     * 幻读√
     */
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),

    /**
     * Read uncommitted
     * 读未提交，即使一个更新语句没有提交，但是别的事务可以读到这个改变
     * 允许读取数据库中未提交的数据更改，也称为脏读
     * 脏读√
     * 不可重复读√
     * 幻读√
     */
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
    /**
     * Repeatable read
     * 可以重复读，同一个事务里面先后执行同一个查询语句的时候，得到的结果是一样的
     * 在同一个事务内的查询都是事务开始时刻一致的，InnoDB默认级别
     * 脏读×
     * 不可重复读×
     * 幻读√
     */
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),
    /**
     * Serializable
     * 序列化，这个事务执行的时候不允许别的事务并发执行，串行化的读，每次读都需要获得表级共享锁，读写相互都会阻塞
     * 脏读×
     * 不可重复读×
     * 幻读×
     */
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);

    private final int level;

    TransactionIsolationLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
