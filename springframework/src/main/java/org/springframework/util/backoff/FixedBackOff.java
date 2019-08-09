package org.springframework.util.backoff;

public class FixedBackOff implements BackOff {

    public static final long DEFAULT_INTERVAL = 5000;

    public static final long UNLIMITED_ATTEMPTS = Long.MAX_VALUE;

    private long interval = DEFAULT_INTERVAL;

    private long maxAttempts = UNLIMITED_ATTEMPTS;

    public FixedBackOff() {
    }

    public FixedBackOff(long interval, long maxAttempts) {
        this.interval = interval;
        this.maxAttempts = maxAttempts;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public long getInterval() {
        return this.interval;
    }

    public void setMaxAttempts(long maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getMaxAttempts() {
        return this.maxAttempts;
    }

    @Override
    public BackOffExecution start() {
        return new FixedBackOffExecution();
    }

    private class FixedBackOffExecution implements BackOffExecution {

        private long currentAttempts = 0;

        @Override
        public long nextBackOff() {
            this.currentAttempts++;
            if (this.currentAttempts <= getMaxAttempts()) {
                return getInterval();
            } else {
                return STOP;
            }
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("FixedBackOff{");
            sb.append("interval=").append(FixedBackOff.this.interval);
            String attemptValue = (FixedBackOff.this.maxAttempts == Long.MAX_VALUE ? "unlimited" : String.valueOf(FixedBackOff.this.maxAttempts));
            sb.append(", currentAttempts=").append(this.currentAttempts);
            sb.append(", maxAttempts=").append(attemptValue);
            sb.append('}');
            return sb.toString();
        }

    }

}
