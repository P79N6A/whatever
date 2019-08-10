package com.alipay.remoting;

import com.alipay.remoting.config.ConfigManager;
import com.alipay.remoting.config.switches.GlobalSwitch;
import com.alipay.remoting.connection.ConnectionFactory;
import com.alipay.remoting.constant.Constants;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.log.BoltLoggerFactory;
import com.alipay.remoting.util.FutureTaskUtil;
import com.alipay.remoting.util.RunStateRecordedFutureTask;
import com.alipay.remoting.util.StringUtils;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.*;

public class DefaultConnectionManager extends AbstractLifeCycle implements ConnectionManager, ConnectionHeartbeatManager, Scannable, LifeCycle {

    private static final Logger logger = BoltLoggerFactory.getLogger("CommonDefault");

    private ThreadPoolExecutor asyncCreateConnectionExecutor;

    private GlobalSwitch globalSwitch;

    protected ConcurrentHashMap<String, RunStateRecordedFutureTask<ConnectionPool>> connTasks;

    protected ConcurrentHashMap<String, FutureTask<Integer>> healTasks;

    protected ConnectionSelectStrategy connectionSelectStrategy;

    protected RemotingAddressParser addressParser;

    protected ConnectionFactory connectionFactory;

    protected ConnectionEventHandler connectionEventHandler;

    protected ConnectionEventListener connectionEventListener;

    public DefaultConnectionManager() {
        this.connTasks = new ConcurrentHashMap<>();
        this.healTasks = new ConcurrentHashMap<String, FutureTask<Integer>>();
        this.connectionSelectStrategy = new RandomSelectStrategy(globalSwitch);
    }

    public DefaultConnectionManager(ConnectionSelectStrategy connectionSelectStrategy) {
        this();
        this.connectionSelectStrategy = connectionSelectStrategy;
    }

    public DefaultConnectionManager(ConnectionSelectStrategy connectionSelectStrategy, ConnectionFactory connectionFactory) {
        this(connectionSelectStrategy);
        this.connectionFactory = connectionFactory;
    }

    public DefaultConnectionManager(ConnectionFactory connectionFactory, RemotingAddressParser addressParser, ConnectionEventHandler connectionEventHandler) {
        this(new RandomSelectStrategy(null), connectionFactory);
        this.addressParser = addressParser;
        this.connectionEventHandler = connectionEventHandler;
    }

    public DefaultConnectionManager(ConnectionSelectStrategy connectionSelectStrategy, ConnectionFactory connectionFactory, ConnectionEventHandler connectionEventHandler, ConnectionEventListener connectionEventListener) {
        this(connectionSelectStrategy, connectionFactory);
        this.connectionEventHandler = connectionEventHandler;
        this.connectionEventListener = connectionEventListener;
    }

    public DefaultConnectionManager(ConnectionSelectStrategy connectionSelectStrategy, ConnectionFactory connectionFactory, ConnectionEventHandler connectionEventHandler, ConnectionEventListener connectionEventListener, GlobalSwitch globalSwitch) {
        this(connectionSelectStrategy, connectionFactory, connectionEventHandler, connectionEventListener);
        this.globalSwitch = globalSwitch;
    }

    @Override
    public void startup() throws LifeCycleException {
        super.startup();
        long keepAliveTime = ConfigManager.conn_create_tp_keepalive();
        int queueSize = ConfigManager.conn_create_tp_queue_size();
        int minPoolSize = ConfigManager.conn_create_tp_min_size();
        int maxPoolSize = ConfigManager.conn_create_tp_max_size();
        this.asyncCreateConnectionExecutor = new ThreadPoolExecutor(minPoolSize, maxPoolSize, keepAliveTime, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(queueSize), new NamedThreadFactory("Bolt-conn-warmup-executor", true));
    }

    @Override
    public void shutdown() throws LifeCycleException {
        super.shutdown();
        if (asyncCreateConnectionExecutor != null) {
            asyncCreateConnectionExecutor.shutdown();
        }
        if (null == this.connTasks || this.connTasks.isEmpty()) {
            return;
        }
        Iterator<String> iter = this.connTasks.keySet().iterator();
        while (iter.hasNext()) {
            String poolKey = iter.next();
            this.removeTask(poolKey);
            iter.remove();
        }
        logger.warn("All connection pool and connections have been removed!");
    }

    @Deprecated
    @Override
    public void init() {
        this.connectionEventHandler.setConnectionManager(this);
        this.connectionEventHandler.setConnectionEventListener(connectionEventListener);
        // 初始化
        this.connectionFactory.init(connectionEventHandler);
    }

    @Override
    public void add(Connection connection) {
        Set<String> poolKeys = connection.getPoolKeys();
        for (String poolKey : poolKeys) {
            this.add(connection, poolKey);
        }
    }

    @Override
    public void add(Connection connection, String poolKey) {
        ConnectionPool pool = null;
        try {
            pool = this.getConnectionPoolAndCreateIfAbsent(poolKey, new ConnectionPoolCall());
        } catch (Exception e) {
            logger.error("[NOTIFYME] Exception occurred when getOrCreateIfAbsent an empty ConnectionPool!", e);
        }
        if (pool != null) {
            pool.add(connection);
        } else {
            logger.error("[NOTIFYME] Connection pool NULL!");
        }
    }

    @Override
    public Connection get(String poolKey) {
        ConnectionPool pool = this.getConnectionPool(this.connTasks.get(poolKey));
        return null == pool ? null : pool.get();
    }

    @Override
    public List<Connection> getAll(String poolKey) {
        ConnectionPool pool = this.getConnectionPool(this.connTasks.get(poolKey));
        return null == pool ? new ArrayList<Connection>() : pool.getAll();
    }

    @Override
    public Map<String, List<Connection>> getAll() {
        Map<String, List<Connection>> allConnections = new HashMap<String, List<Connection>>();
        Iterator<Map.Entry<String, RunStateRecordedFutureTask<ConnectionPool>>> iterator = this.getConnPools().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, RunStateRecordedFutureTask<ConnectionPool>> entry = iterator.next();
            ConnectionPool pool = FutureTaskUtil.getFutureTaskResult(entry.getValue(), logger);
            if (null != pool) {
                allConnections.put(entry.getKey(), pool.getAll());
            }
        }
        return allConnections;
    }

    @Override
    public void remove(Connection connection) {
        if (null == connection) {
            return;
        }
        Set<String> poolKeys = connection.getPoolKeys();
        if (null == poolKeys || poolKeys.isEmpty()) {
            connection.close();
            logger.warn("Remove and close a standalone connection.");
        } else {
            for (String poolKey : poolKeys) {
                this.remove(connection, poolKey);
            }
        }
    }

    @Override
    public void remove(Connection connection, String poolKey) {
        if (null == connection || StringUtils.isBlank(poolKey)) {
            return;
        }
        ConnectionPool pool = this.getConnectionPool(this.connTasks.get(poolKey));
        if (null == pool) {
            connection.close();
            logger.warn("Remove and close a standalone connection.");
        } else {
            pool.removeAndTryClose(connection);
            if (pool.isEmpty()) {
                this.removeTask(poolKey);
                logger.warn("Remove and close the last connection in ConnectionPool with poolKey {}", poolKey);
            } else {
                logger.warn("Remove and close a connection in ConnectionPool with poolKey {}, {} connections left.", poolKey, pool.size());
            }
        }
    }

    @Override
    public void remove(String poolKey) {
        if (StringUtils.isBlank(poolKey)) {
            return;
        }
        RunStateRecordedFutureTask<ConnectionPool> task = this.connTasks.remove(poolKey);
        if (null != task) {
            ConnectionPool pool = this.getConnectionPool(task);
            if (null != pool) {
                pool.removeAllAndTryClose();
                logger.warn("Remove and close all connections in ConnectionPool of poolKey={}", poolKey);
            }
        }
    }

    @Deprecated
    @Override
    public void removeAll() {
        if (null == this.connTasks || this.connTasks.isEmpty()) {
            return;
        }
        Iterator<String> iter = this.connTasks.keySet().iterator();
        while (iter.hasNext()) {
            String poolKey = iter.next();
            this.removeTask(poolKey);
            iter.remove();
        }
        logger.warn("All connection pool and connections have been removed!");
    }

    @Override
    public void check(Connection connection) throws RemotingException {
        if (connection == null) {
            throw new RemotingException("Connection is null when do check!");
        }
        if (connection.getChannel() == null || !connection.getChannel().isActive()) {
            this.remove(connection);
            throw new RemotingException("Check connection failed for address: " + connection.getUrl());
        }
        if (!connection.getChannel().isWritable()) {
            throw new RemotingException("Check connection failed for address: " + connection.getUrl() + ", maybe write overflow!");
        }
    }

    @Override
    public int count(String poolKey) {
        if (StringUtils.isBlank(poolKey)) {
            return 0;
        }
        ConnectionPool pool = this.getConnectionPool(this.connTasks.get(poolKey));
        if (null != pool) {
            return pool.size();
        } else {
            return 0;
        }
    }

    @Override
    public void scan() {
        if (null != this.connTasks && !this.connTasks.isEmpty()) {
            Iterator<String> iter = this.connTasks.keySet().iterator();
            while (iter.hasNext()) {
                String poolKey = iter.next();
                ConnectionPool pool = this.getConnectionPool(this.connTasks.get(poolKey));
                if (null != pool) {
                    pool.scan();
                    if (pool.isEmpty()) {
                        if ((System.currentTimeMillis() - pool.getLastAccessTimestamp()) > Constants.DEFAULT_EXPIRE_TIME) {
                            iter.remove();
                            logger.warn("Remove expired pool task of poolKey {} which is empty.", poolKey);
                        }
                    }
                }
            }
        }
    }

    @Override
    public Connection getAndCreateIfAbsent(Url url) throws InterruptedException, RemotingException {
        // 获取连接池
        ConnectionPool pool = this.getConnectionPoolAndCreateIfAbsent(url.getUniqueKey(),
                // Callable任务，获取连接池
                new ConnectionPoolCall(url));
        if (null != pool) {
            // 从连接池获取连接
            return pool.get();
        } else {
            logger.error("[NOTIFYME] bug detected! pool here must not be null!");
            return null;
        }
    }

    @Override
    public void createConnectionAndHealIfNeed(Url url) throws InterruptedException, RemotingException {
        ConnectionPool pool = this.getConnectionPoolAndCreateIfAbsent(url.getUniqueKey(), new ConnectionPoolCall(url));
        if (null != pool) {
            healIfNeed(pool, url);
        } else {
            logger.error("[NOTIFYME] bug detected! pool here must not be null!");
        }
    }

    @Override
    public Connection create(Url url) throws RemotingException {
        Connection conn;
        try {
            // 连接工厂创建
            conn = this.connectionFactory.createConnection(url);
        } catch (Exception e) {
            throw new RemotingException("Create connection failed. The address is " + url.getOriginUrl(), e);
        }
        return conn;
    }

    @Override
    public Connection create(String ip, int port, int connectTimeout) throws RemotingException {
        try {
            return this.connectionFactory.createConnection(ip, port, connectTimeout);
        } catch (Exception e) {
            throw new RemotingException("Create connection failed. The address is " + ip + ":" + port, e);
        }
    }

    @Override
    public Connection create(String address, int connectTimeout) throws RemotingException {
        Url url = this.addressParser.parse(address);
        url.setConnectTimeout(connectTimeout);
        return create(url);
    }

    @Override
    public void disableHeartbeat(Connection connection) {
        if (null != connection) {
            connection.getChannel().attr(Connection.HEARTBEAT_SWITCH).set(false);
        }
    }

    @Override
    public void enableHeartbeat(Connection connection) {
        if (null != connection) {
            connection.getChannel().attr(Connection.HEARTBEAT_SWITCH).set(true);
        }
    }

    private ConnectionPool getConnectionPool(RunStateRecordedFutureTask<ConnectionPool> task) {
        return FutureTaskUtil.getFutureTaskResult(task, logger);

    }

    /**
     * 获取连接池
     */
    private ConnectionPool getConnectionPoolAndCreateIfAbsent(String poolKey, Callable<ConnectionPool> callable) throws RemotingException, InterruptedException {
        //
        RunStateRecordedFutureTask<ConnectionPool> initialTask;
        // 连接池
        ConnectionPool pool = null;
        // 重试次数
        int retry = Constants.DEFAULT_RETRY_TIMES;
        int timesOfResultNull = 0;
        int timesOfInterrupt = 0;
        // 重试
        for (int i = 0; (i < retry) && (pool == null); ++i) {
            // 每个key一个任务
            initialTask = this.connTasks.get(poolKey);
            if (null == initialTask) {
                // 没有就创建
                RunStateRecordedFutureTask<ConnectionPool> newTask = new RunStateRecordedFutureTask<ConnectionPool>(callable);
                initialTask = this.connTasks.putIfAbsent(poolKey, newTask);
                if (null == initialTask) {
                    initialTask = newTask;
                    // 执行Callable任务
                    initialTask.run();
                }
            }
            try {
                // 阻塞获取连接池
                pool = initialTask.get();
                if (null == pool) {
                    if (i + 1 < retry) {
                        timesOfResultNull++;
                        continue;
                    }
                    this.connTasks.remove(poolKey);
                    String errMsg = "Get future task result null for poolKey [" + poolKey + "] after [" + (timesOfResultNull + 1) + "] times try.";
                    throw new RemotingException(errMsg);
                }
            } catch (InterruptedException e) {
                if (i + 1 < retry) {
                    timesOfInterrupt++;
                    continue;
                }
                this.connTasks.remove(poolKey);
                logger.warn("Future task of poolKey {} interrupted {} times. InterruptedException thrown and stop retry.", poolKey, (timesOfInterrupt + 1), e);
                throw e;
            } catch (ExecutionException e) {
                this.connTasks.remove(poolKey);
                Throwable cause = e.getCause();
                if (cause instanceof RemotingException) {
                    throw (RemotingException) cause;
                } else {
                    FutureTaskUtil.launderThrowable(cause);
                }
            }
        }
        return pool;
    }

    protected void removeTask(String poolKey) {
        RunStateRecordedFutureTask<ConnectionPool> task = this.connTasks.remove(poolKey);
        if (null != task) {
            ConnectionPool pool = FutureTaskUtil.getFutureTaskResult(task, logger);
            if (null != pool) {
                pool.removeAllAndTryClose();
            }
        }
    }

    private void healIfNeed(ConnectionPool pool, Url url) throws RemotingException, InterruptedException {
        String poolKey = url.getUniqueKey();
        if (pool.isAsyncCreationDone() && pool.size() < url.getConnNum()) {
            FutureTask<Integer> task = this.healTasks.get(poolKey);
            if (null == task) {
                FutureTask<Integer> newTask = new FutureTask<Integer>(new HealConnectionCall(url, pool));
                task = this.healTasks.putIfAbsent(poolKey, newTask);
                if (null == task) {
                    task = newTask;
                    task.run();
                }
            }
            try {
                int numAfterHeal = task.get();
                if (logger.isDebugEnabled()) {
                    logger.debug("[NOTIFYME] - conn num after heal {}, expected {}, warmup {}", numAfterHeal, url.getConnNum(), url.isConnWarmup());
                }
            } catch (InterruptedException e) {
                this.healTasks.remove(poolKey);
                throw e;
            } catch (ExecutionException e) {
                this.healTasks.remove(poolKey);
                Throwable cause = e.getCause();
                if (cause instanceof RemotingException) {
                    throw (RemotingException) cause;
                } else {
                    FutureTaskUtil.launderThrowable(cause);
                }
            }
            this.healTasks.remove(poolKey);
        }
    }

    /**
     * 包装获取连接池的操作
     */
    private class ConnectionPoolCall implements Callable<ConnectionPool> {
        private boolean whetherInitConnection;

        private Url url;

        public ConnectionPoolCall() {
            this.whetherInitConnection = false;
        }

        public ConnectionPoolCall(Url url) {
            this.whetherInitConnection = true;
            this.url = url;
        }

        @Override
        public ConnectionPool call() throws Exception {
            final ConnectionPool pool = new ConnectionPool(connectionSelectStrategy);
            if (whetherInitConnection) {
                try {
                    doCreate(this.url, pool, this.getClass().getSimpleName(), 1);
                } catch (Exception e) {
                    pool.removeAllAndTryClose();
                    throw e;
                }
            }
            return pool;
        }

    }

    private class HealConnectionCall implements Callable<Integer> {
        private Url url;

        private ConnectionPool pool;

        public HealConnectionCall(Url url, ConnectionPool pool) {
            this.url = url;
            this.pool = pool;
        }

        @Override
        public Integer call() throws Exception {
            doCreate(this.url, this.pool, this.getClass().getSimpleName(), 0);
            return this.pool.size();
        }

    }

    private void doCreate(final Url url, final ConnectionPool pool, final String taskName, final int syncCreateNumWhenNotWarmup) throws RemotingException {

        // 现存连接数
        final int actualNum = pool.size();
        // 期望连接数
        final int expectNum = url.getConnNum();
        if (actualNum >= expectNum) {
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("actual num {}, expect num {}, task name {}", actualNum, expectNum, taskName);
        }
        // 连接热身
        if (url.isConnWarmup()) {
            for (int i = actualNum; i < expectNum; ++i) {
                // 马上创建连接
                Connection connection = create(url);
                pool.add(connection);
            }
        }
        // 异步创建
        else {
            if (syncCreateNumWhenNotWarmup < 0 || syncCreateNumWhenNotWarmup > url.getConnNum()) {
                throw new IllegalArgumentException("sync create number when not warmup should be [0," + url.getConnNum() + "]");
            }
            if (syncCreateNumWhenNotWarmup > 0) {
                for (int i = 0; i < syncCreateNumWhenNotWarmup; ++i) {
                    // 先创建需要同步创建的
                    Connection connection = create(url);
                    pool.add(connection);
                }
                // 够了
                if (syncCreateNumWhenNotWarmup >= url.getConnNum()) {
                    return;
                }
            }
            // 标记异步创建开始
            pool.markAsyncCreationStart();
            try {
                // 线程池执行
                this.asyncCreateConnectionExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            for (int i = pool.size(); i < url.getConnNum(); ++i) {
                                Connection conn = null;
                                try {
                                    conn = create(url);
                                } catch (RemotingException e) {
                                    logger.error("Exception occurred in async create connection thread for {}, taskName {}", url.getUniqueKey(), taskName, e);
                                }
                                pool.add(conn);
                            }
                        } finally {
                            // 标记异步创建结束
                            pool.markAsyncCreationDone();
                        }
                    }
                });
            } catch (RejectedExecutionException e) {
                pool.markAsyncCreationDone();
                throw e;
            }
        }
    }

    public ConnectionSelectStrategy getConnectionSelectStrategy() {
        return connectionSelectStrategy;
    }

    public void setConnectionSelectStrategy(ConnectionSelectStrategy connectionSelectStrategy) {
        this.connectionSelectStrategy = connectionSelectStrategy;
    }

    public RemotingAddressParser getAddressParser() {
        return addressParser;
    }

    public void setAddressParser(RemotingAddressParser addressParser) {
        this.addressParser = addressParser;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public ConnectionEventHandler getConnectionEventHandler() {
        return connectionEventHandler;
    }

    public void setConnectionEventHandler(ConnectionEventHandler connectionEventHandler) {
        this.connectionEventHandler = connectionEventHandler;
    }

    public ConnectionEventListener getConnectionEventListener() {
        return connectionEventListener;
    }

    public void setConnectionEventListener(ConnectionEventListener connectionEventListener) {
        this.connectionEventListener = connectionEventListener;
    }

    public ConcurrentHashMap<String, RunStateRecordedFutureTask<ConnectionPool>> getConnPools() {
        return this.connTasks;
    }

}
