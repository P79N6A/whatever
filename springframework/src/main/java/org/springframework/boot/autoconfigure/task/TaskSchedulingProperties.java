package org.springframework.boot.autoconfigure.task;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("spring.task.scheduling")
public class TaskSchedulingProperties {

    private final Pool pool = new Pool();

    private final Shutdown shutdown = new Shutdown();

    private String threadNamePrefix = "scheduling-";

    public Pool getPool() {
        return this.pool;
    }

    public Shutdown getShutdown() {
        return this.shutdown;
    }

    public String getThreadNamePrefix() {
        return this.threadNamePrefix;
    }

    public void setThreadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

    public static class Pool {

        private int size = 1;

        public int getSize() {
            return this.size;
        }

        public void setSize(int size) {
            this.size = size;
        }

    }

    public static class Shutdown {

        private boolean awaitTermination;

        private Duration awaitTerminationPeriod;

        public boolean isAwaitTermination() {
            return this.awaitTermination;
        }

        public void setAwaitTermination(boolean awaitTermination) {
            this.awaitTermination = awaitTermination;
        }

        public Duration getAwaitTerminationPeriod() {
            return this.awaitTerminationPeriod;
        }

        public void setAwaitTerminationPeriod(Duration awaitTerminationPeriod) {
            this.awaitTerminationPeriod = awaitTerminationPeriod;
        }

    }

}
