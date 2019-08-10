package com.alipay.remoting;

import com.alipay.remoting.log.BoltLoggerFactory;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class ReconnectManager extends AbstractLifeCycle implements Reconnector {

    private static final Logger logger = BoltLoggerFactory.getLogger("CommonDefault");

    private static final int HEAL_CONNECTION_INTERVAL = 1000;

    private final ConnectionManager connectionManager;

    private final LinkedBlockingQueue<ReconnectTask> tasks;

    private final List<Url> canceled;

    private Thread healConnectionThreads;

    public ReconnectManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.tasks = new LinkedBlockingQueue<ReconnectTask>();
        this.canceled = new CopyOnWriteArrayList<Url>();
    }

    @Override
    public void reconnect(Url url) {
        tasks.add(new ReconnectTask(url));
    }

    @Override
    public void disableReconnect(Url url) {
        canceled.add(url);
    }

    @Override
    public void enableReconnect(Url url) {
        canceled.remove(url);
    }

    @Override
    public void startup() throws LifeCycleException {
        super.startup();
        this.healConnectionThreads = new Thread(new HealConnectionRunner());
        this.healConnectionThreads.start();
    }

    @Override
    public void shutdown() throws LifeCycleException {
        super.shutdown();
        healConnectionThreads.interrupt();
        this.tasks.clear();
        this.canceled.clear();
    }

    @Deprecated
    public void addCancelUrl(Url url) {
        disableReconnect(url);
    }

    @Deprecated
    public void removeCancelUrl(Url url) {
        enableReconnect(url);
    }

    @Deprecated
    public void addReconnectTask(Url url) {
        reconnect(url);
    }

    @Deprecated
    public void stop() {
        shutdown();
    }

    private final class HealConnectionRunner implements Runnable {
        private long lastConnectTime = -1;

        @Override
        public void run() {
            while (isStarted()) {
                long start = -1;
                ReconnectTask task = null;
                try {
                    if (this.lastConnectTime < HEAL_CONNECTION_INTERVAL) {
                        Thread.sleep(HEAL_CONNECTION_INTERVAL);
                    }
                    try {
                        task = ReconnectManager.this.tasks.take();
                    } catch (InterruptedException e) {
                    }
                    if (task == null) {
                        continue;
                    }
                    start = System.currentTimeMillis();
                    if (!canceled.contains(task.url)) {
                        task.run();
                    } else {
                        logger.warn("Invalid reconnect request task {}, cancel list size {}", task.url, canceled.size());
                    }
                    this.lastConnectTime = System.currentTimeMillis() - start;
                } catch (Exception e) {
                    if (start != -1) {
                        this.lastConnectTime = System.currentTimeMillis() - start;
                    }
                    if (task != null) {
                        logger.warn("reconnect target: {} failed.", task.url, e);
                        tasks.add(task);
                    }
                }
            }
        }

    }

    private class ReconnectTask implements Runnable {
        Url url;

        public ReconnectTask(Url url) {
            this.url = url;
        }

        @Override
        public void run() {
            try {
                connectionManager.createConnectionAndHealIfNeed(url);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

}
