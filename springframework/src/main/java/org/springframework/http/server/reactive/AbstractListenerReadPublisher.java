package org.springframework.http.server.reactive;

import org.apache.commons.logging.Log;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.core.log.LogDelegateFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Operators;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractListenerReadPublisher<T> implements Publisher<T> {

    protected static Log rsReadLogger = LogDelegateFactory.getHiddenLog(AbstractListenerReadPublisher.class);

    private final AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

    private volatile long demand;

    @SuppressWarnings("rawtypes")
    private static final AtomicLongFieldUpdater<AbstractListenerReadPublisher> DEMAND_FIELD_UPDATER = AtomicLongFieldUpdater.newUpdater(AbstractListenerReadPublisher.class, "demand");

    @Nullable
    private volatile Subscriber<? super T> subscriber;

    private volatile boolean completionBeforeDemand;

    @Nullable
    private volatile Throwable errorBeforeDemand;

    private final String logPrefix;

    public AbstractListenerReadPublisher() {
        this("");
    }

    public AbstractListenerReadPublisher(String logPrefix) {
        this.logPrefix = logPrefix;
    }

    public String getLogPrefix() {
        return this.logPrefix;
    }
    // Publisher implementation...

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        this.state.get().subscribe(this, subscriber);
    }
    // Async I/O notification methods...

    public final void onDataAvailable() {
        rsReadLogger.trace(getLogPrefix() + "onDataAvailable");
        this.state.get().onDataAvailable(this);
    }

    public void onAllDataRead() {
        rsReadLogger.trace(getLogPrefix() + "onAllDataRead");
        this.state.get().onAllDataRead(this);
    }

    public final void onError(Throwable ex) {
        if (rsReadLogger.isTraceEnabled()) {
            rsReadLogger.trace(getLogPrefix() + "Connection error: " + ex);
        }
        this.state.get().onError(this, ex);
    }
    // Read API methods to be implemented or template methods to override...

    protected abstract void checkOnDataAvailable();

    @Nullable
    protected abstract T read() throws IOException;

    protected abstract void readingPaused();

    protected abstract void discardData();
    // Private methods for use in State...

    private boolean readAndPublish() throws IOException {
        long r;
        while ((r = this.demand) > 0 && !this.state.get().equals(State.COMPLETED)) {
            T data = read();
            if (data != null) {
                if (r != Long.MAX_VALUE) {
                    DEMAND_FIELD_UPDATER.addAndGet(this, -1L);
                }
                Subscriber<? super T> subscriber = this.subscriber;
                Assert.state(subscriber != null, "No subscriber");
                if (rsReadLogger.isTraceEnabled()) {
                    rsReadLogger.trace(getLogPrefix() + "Publishing data read");
                }
                subscriber.onNext(data);
            } else {
                if (rsReadLogger.isTraceEnabled()) {
                    rsReadLogger.trace(getLogPrefix() + "No more data to read");
                }
                return true;
            }
        }
        return false;
    }

    private boolean changeState(State oldState, State newState) {
        boolean result = this.state.compareAndSet(oldState, newState);
        if (result && rsReadLogger.isTraceEnabled()) {
            rsReadLogger.trace(getLogPrefix() + oldState + " -> " + newState);
        }
        return result;
    }

    private void changeToDemandState(State oldState) {
        if (changeState(oldState, State.DEMAND)) {
            // Protect from infinite recursion in Undertow, where we can't check if data
            // is available, so all we can do is to try to read.
            // Generally, no need to check if we just came out of readAndPublish()...
            if (!oldState.equals(State.READING)) {
                checkOnDataAvailable();
            }
        }
    }

    private Subscription createSubscription() {
        return new ReadSubscription();
    }

    private final class ReadSubscription implements Subscription {

        @Override
        public final void request(long n) {
            if (rsReadLogger.isTraceEnabled()) {
                rsReadLogger.trace(getLogPrefix() + n + " requested");
            }
            state.get().request(AbstractListenerReadPublisher.this, n);
        }

        @Override
        public final void cancel() {
            if (rsReadLogger.isTraceEnabled()) {
                rsReadLogger.trace(getLogPrefix() + "Cancellation");
            }
            state.get().cancel(AbstractListenerReadPublisher.this);
        }

    }

    private enum State {

        UNSUBSCRIBED {
            @Override
            <T> void subscribe(AbstractListenerReadPublisher<T> publisher, Subscriber<? super T> subscriber) {
                Assert.notNull(publisher, "Publisher must not be null");
                Assert.notNull(subscriber, "Subscriber must not be null");
                if (publisher.changeState(this, SUBSCRIBING)) {
                    Subscription subscription = publisher.createSubscription();
                    publisher.subscriber = subscriber;
                    subscriber.onSubscribe(subscription);
                    publisher.changeState(SUBSCRIBING, NO_DEMAND);
                    // Now safe to check "beforeDemand" flags, they won't change once in NO_DEMAND
                    String logPrefix = publisher.getLogPrefix();
                    if (publisher.completionBeforeDemand) {
                        rsReadLogger.trace(logPrefix + "Completed before demand");
                        publisher.state.get().onAllDataRead(publisher);
                    }
                    Throwable ex = publisher.errorBeforeDemand;
                    if (ex != null) {
                        if (rsReadLogger.isTraceEnabled()) {
                            rsReadLogger.trace(logPrefix + "Completed with error before demand: " + ex);
                        }
                        publisher.state.get().onError(publisher, ex);
                    }
                } else {
                    throw new IllegalStateException("Failed to transition to SUBSCRIBING, " + "subscriber: " + subscriber);
                }
            }

            @Override
            <T> void onAllDataRead(AbstractListenerReadPublisher<T> publisher) {
                publisher.completionBeforeDemand = true;
            }

            @Override
            <T> void onError(AbstractListenerReadPublisher<T> publisher, Throwable ex) {
                publisher.errorBeforeDemand = ex;
            }
        },

        SUBSCRIBING {
            @Override
            <T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
                if (Operators.validate(n)) {
                    Operators.addCap(DEMAND_FIELD_UPDATER, publisher, n);
                    publisher.changeToDemandState(this);
                }
            }

            @Override
            <T> void onAllDataRead(AbstractListenerReadPublisher<T> publisher) {
                publisher.completionBeforeDemand = true;
            }

            @Override
            <T> void onError(AbstractListenerReadPublisher<T> publisher, Throwable ex) {
                publisher.errorBeforeDemand = ex;
            }
        },

        NO_DEMAND {
            @Override
            <T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
                if (Operators.validate(n)) {
                    Operators.addCap(DEMAND_FIELD_UPDATER, publisher, n);
                    publisher.changeToDemandState(this);
                }
            }
        },

        DEMAND {
            @Override
            <T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
                if (Operators.validate(n)) {
                    Operators.addCap(DEMAND_FIELD_UPDATER, publisher, n);
                    // Did a concurrent read transition to NO_DEMAND just before us?
                    publisher.changeToDemandState(NO_DEMAND);
                }
            }

            @Override
            <T> void onDataAvailable(AbstractListenerReadPublisher<T> publisher) {
                if (publisher.changeState(this, READING)) {
                    try {
                        boolean demandAvailable = publisher.readAndPublish();
                        if (demandAvailable) {
                            publisher.changeToDemandState(READING);
                        } else {
                            publisher.readingPaused();
                            if (publisher.changeState(READING, NO_DEMAND)) {
                                // Demand may have arrived since readAndPublish returned
                                long r = publisher.demand;
                                if (r > 0) {
                                    publisher.changeToDemandState(NO_DEMAND);
                                }
                            }
                        }
                    } catch (IOException ex) {
                        publisher.onError(ex);
                    }
                }
                // Else, either competing onDataAvailable (request vs container), or concurrent completion
            }
        },

        READING {
            @Override
            <T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
                if (Operators.validate(n)) {
                    Operators.addCap(DEMAND_FIELD_UPDATER, publisher, n);
                    // Did a concurrent read transition to NO_DEMAND just before us?
                    publisher.changeToDemandState(NO_DEMAND);
                }
            }
        },

        COMPLETED {
            @Override
            <T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
                // ignore
            }

            @Override
            <T> void cancel(AbstractListenerReadPublisher<T> publisher) {
                // ignore
            }

            @Override
            <T> void onAllDataRead(AbstractListenerReadPublisher<T> publisher) {
                // ignore
            }

            @Override
            <T> void onError(AbstractListenerReadPublisher<T> publisher, Throwable t) {
                // ignore
            }
        };

        <T> void subscribe(AbstractListenerReadPublisher<T> publisher, Subscriber<? super T> subscriber) {
            throw new IllegalStateException(toString());
        }

        <T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
            throw new IllegalStateException(toString());
        }

        <T> void cancel(AbstractListenerReadPublisher<T> publisher) {
            if (publisher.changeState(this, COMPLETED)) {
                publisher.discardData();
            } else {
                publisher.state.get().cancel(publisher);
            }
        }

        <T> void onDataAvailable(AbstractListenerReadPublisher<T> publisher) {
            // ignore
        }

        <T> void onAllDataRead(AbstractListenerReadPublisher<T> publisher) {
            if (publisher.changeState(this, COMPLETED)) {
                Subscriber<? super T> s = publisher.subscriber;
                if (s != null) {
                    s.onComplete();
                }
            } else {
                publisher.state.get().onAllDataRead(publisher);
            }
        }

        <T> void onError(AbstractListenerReadPublisher<T> publisher, Throwable t) {
            if (publisher.changeState(this, COMPLETED)) {
                publisher.discardData();
                Subscriber<? super T> s = publisher.subscriber;
                if (s != null) {
                    s.onError(t);
                }
            } else {
                publisher.state.get().onError(publisher, t);
            }
        }
    }

}
