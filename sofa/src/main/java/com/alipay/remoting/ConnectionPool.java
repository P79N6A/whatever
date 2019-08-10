package com.alipay.remoting;

import com.alipay.remoting.log.BoltLoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConnectionPool implements Scannable {

    private static final Logger logger = BoltLoggerFactory.getLogger("CommonDefault");

    private CopyOnWriteArrayList<Connection> connections;

    private ConnectionSelectStrategy strategy;

    private volatile long lastAccessTimestamp;

    private volatile boolean asyncCreationDone;

    public ConnectionPool(ConnectionSelectStrategy strategy) {
        this.strategy = strategy;
        this.connections = new CopyOnWriteArrayList<Connection>();
        this.asyncCreationDone = true;
    }

    public void add(Connection connection) {
        markAccess();
        if (null == connection) {
            return;
        }
        boolean res = connections.addIfAbsent(connection);
        if (res) {
            connection.increaseRef();
        }
    }

    public boolean contains(Connection connection) {
        return connections.contains(connection);
    }

    public void removeAndTryClose(Connection connection) {
        if (null == connection) {
            return;
        }
        boolean res = connections.remove(connection);
        if (res) {
            connection.decreaseRef();
        }
        if (connection.noRef()) {
            connection.close();
        }
    }

    public void removeAllAndTryClose() {
        for (Connection conn : connections) {
            removeAndTryClose(conn);
        }
        connections.clear();
    }

    public Connection get() {
        markAccess();
        if (null != connections) {
            List<Connection> snapshot = new ArrayList<Connection>(connections);
            if (snapshot.size() > 0) {
                return strategy.select(snapshot);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public List<Connection> getAll() {
        markAccess();
        return new ArrayList<Connection>(connections);
    }

    public int size() {
        return connections.size();
    }

    public boolean isEmpty() {
        return connections.isEmpty();
    }

    public long getLastAccessTimestamp() {
        return lastAccessTimestamp;
    }

    private void markAccess() {
        lastAccessTimestamp = System.currentTimeMillis();
    }

    public boolean isAsyncCreationDone() {
        return asyncCreationDone;
    }

    public void markAsyncCreationDone() {
        asyncCreationDone = true;
    }

    public void markAsyncCreationStart() {
        asyncCreationDone = false;
    }

    @Override
    public void scan() {
        if (null != connections && !connections.isEmpty()) {
            for (Connection conn : connections) {
                if (!conn.isFine()) {
                    logger.warn("Remove bad connection when scanning conns of ConnectionPool - {}:{}", conn.getRemoteIP(), conn.getRemotePort());
                    conn.close();
                    removeAndTryClose(conn);
                }
            }
        }
    }

}
