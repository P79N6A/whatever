package org.springframework.util.backoff;

import org.springframework.util.Assert;

public class ExponentialBackOff implements BackOff {

    public static final long DEFAULT_INITIAL_INTERVAL = 2000L;

    public static final double DEFAULT_MULTIPLIER = 1.5;

    public static final long DEFAULT_MAX_INTERVAL = 30000L;

    public static final long DEFAULT_MAX_ELAPSED_TIME = Long.MAX_VALUE;

    private long initialInterval = DEFAULT_INITIAL_INTERVAL;

    private double multiplier = DEFAULT_MULTIPLIER;

    private long maxInterval = DEFAULT_MAX_INTERVAL;

    private long maxElapsedTime = DEFAULT_MAX_ELAPSED_TIME;

    public ExponentialBackOff() {
    }

    public ExponentialBackOff(long initialInterval, double multiplier) {
        checkMultiplier(multiplier);
        this.initialInterval = initialInterval;
        this.multiplier = multiplier;
    }

    public void setInitialInterval(long initialInterval) {
        this.initialInterval = initialInterval;
    }

    public long getInitialInterval() {
        return this.initialInterval;
    }

    public void setMultiplier(double multiplier) {
        checkMultiplier(multiplier);
        this.multiplier = multiplier;
    }

    public double getMultiplier() {
        return this.multiplier;
    }

    public void setMaxInterval(long maxInterval) {
        this.maxInterval = maxInterval;
    }

    public long getMaxInterval() {
        return this.maxInterval;
    }

    public void setMaxElapsedTime(long maxElapsedTime) {
        this.maxElapsedTime = maxElapsedTime;
    }

    public long getMaxElapsedTime() {
        return this.maxElapsedTime;
    }

    @Override
    public BackOffExecution start() {
        return new ExponentialBackOffExecution();
    }

    private void checkMultiplier(double multiplier) {
        Assert.isTrue(multiplier >= 1, () -> "Invalid multiplier '" + multiplier + "'. Should be greater than " + "or equal to 1. A multiplier of 1 is equivalent to a fixed interval.");
    }

    private class ExponentialBackOffExecution implements BackOffExecution {

        private long currentInterval = -1;

        private long currentElapsedTime = 0;

        @Override
        public long nextBackOff() {
            if (this.currentElapsedTime >= maxElapsedTime) {
                return STOP;
            }
            long nextInterval = computeNextInterval();
            this.currentElapsedTime += nextInterval;
            return nextInterval;
        }

        private long computeNextInterval() {
            long maxInterval = getMaxInterval();
            if (this.currentInterval >= maxInterval) {
                return maxInterval;
            } else if (this.currentInterval < 0) {
                long initialInterval = getInitialInterval();
                this.currentInterval = (initialInterval < maxInterval ? initialInterval : maxInterval);
            } else {
                this.currentInterval = multiplyInterval(maxInterval);
            }
            return this.currentInterval;
        }

        private long multiplyInterval(long maxInterval) {
            long i = this.currentInterval;
            i *= getMultiplier();
            return (i > maxInterval ? maxInterval : i);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("ExponentialBackOff{");
            sb.append("currentInterval=").append(this.currentInterval < 0 ? "n/a" : this.currentInterval + "ms");
            sb.append(", multiplier=").append(getMultiplier());
            sb.append('}');
            return sb.toString();
        }

    }

}
