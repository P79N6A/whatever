package org.springframework.http.server.reactive;

import org.apache.commons.logging.Log;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.core.log.LogDelegateFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Operators;

import java.util.concurrent.atomic.AtomicReference;

class WriteResultPublisher implements Publisher<Void> {

    private static final Log rsWriteResultLogger = LogDelegateFactory.getHiddenLog(WriteResultPublisher.class);

    private final AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

    @Nullable
    private volatile Subscriber<? super Void> subscriber;

    private volatile boolean completedBeforeSubscribed;

    @Nullable
    private volatile Throwable errorBeforeSubscribed;

    private final String logPrefix;

    public WriteResultPublisher(String logPrefix) {
        this.logPrefix = logPrefix;
    }

    @Override
    public final void subscribe(Subscriber<? super Void> subscriber) {
        if (rsWriteResultLogger.isTraceEnabled()) {
            rsWriteResultLogger.trace(this.logPrefix + this.state + " subscribe: " + subscriber);
        }
        this.state.get().subscribe(this, subscriber);
    }

    public void publishComplete() {
        if (rsWriteResultLogger.isTraceEnabled()) {
            rsWriteResultLogger.trace(this.logPrefix + this.state + " publishComplete");
        }
        this.state.get().publishComplete(this);
    }

    public void publishError(Throwable t) {
        if (rsWriteResultLogger.isTraceEnabled()) {
            rsWriteResultLogger.trace(this.logPrefix + this.state + " publishError: " + t);
        }
        this.state.get().publishError(this, t);
    }

    private boolean changeState(State oldState, State newState) {
        return this.state.compareAndSet(oldState, newState);
    }

    private static final class WriteResultSubscription implements Subscription {

        private final WriteResultPublisher publisher;

        public WriteResultSubscription(WriteResultPublisher publisher) {
            this.publisher = publisher;
        }

        @Override
        public final void request(long n) {
            if (rsWriteResultLogger.isTraceEnabled()) {
                rsWriteResultLogger.trace(this.publisher.logPrefix + state() + " request: " + n);
            }
            state().request(this.publisher, n);
        }

        @Override
        public final void cancel() {
            if (rsWriteResultLogger.isTraceEnabled()) {
                rsWriteResultLogger.trace(this.publisher.logPrefix + state() + " cancel");
            }
            state().cancel(this.publisher);
        }

        private State state() {
            return this.publisher.state.get();
        }

    }

    private enum State {

        UNSUBSCRIBED {
            @Override
            void subscribe(WriteResultPublisher publisher, Subscriber<? super Void> subscriber) {
                Assert.notNull(subscriber, "Subscriber must not be null");
                if (publisher.changeState(this, SUBSCRIBING)) {
                    Subscription subscription = new WriteResultSubscription(publisher);
                    publisher.subscriber = subscriber;
                    subscriber.onSubscribe(subscription);
                    publisher.changeState(SUBSCRIBING, SUBSCRIBED);
                    // Now safe to check "beforeSubscribed" flags, they won't change once in NO_DEMAND
                    if (publisher.completedBeforeSubscribed) {
                        publisher.publishComplete();
                    }
                    Throwable publisherError = publisher.errorBeforeSubscribed;
                    if (publisherError != null) {
                        publisher.publishError(publisherError);
                    }
                } else {
                    throw new IllegalStateException(toString());
                }
            }

            @Override
            void publishComplete(WriteResultPublisher publisher) {
                publisher.completedBeforeSubscribed = true;
            }

            @Override
            void publishError(WriteResultPublisher publisher, Throwable ex) {
                publisher.errorBeforeSubscribed = ex;
            }
        },

        SUBSCRIBING {
            @Override
            void request(WriteResultPublisher publisher, long n) {
                Operators.validate(n);
            }

            @Override
            void publishComplete(WriteResultPublisher publisher) {
                publisher.completedBeforeSubscribed = true;
            }

            @Override
            void publishError(WriteResultPublisher publisher, Throwable ex) {
                publisher.errorBeforeSubscribed = ex;
            }
        },

        SUBSCRIBED {
            @Override
            void request(WriteResultPublisher publisher, long n) {
                Operators.validate(n);
            }
        },

        COMPLETED {
            @Override
            void request(WriteResultPublisher publisher, long n) {
                // ignore
            }

            @Override
            void cancel(WriteResultPublisher publisher) {
                // ignore
            }

            @Override
            void publishComplete(WriteResultPublisher publisher) {
                // ignore
            }

            @Override
            void publishError(WriteResultPublisher publisher, Throwable t) {
                // ignore
            }
        };

        void subscribe(WriteResultPublisher publisher, Subscriber<? super Void> subscriber) {
            throw new IllegalStateException(toString());
        }

        void request(WriteResultPublisher publisher, long n) {
            throw new IllegalStateException(toString());
        }

        void cancel(WriteResultPublisher publisher) {
            if (!publisher.changeState(this, COMPLETED)) {
                publisher.state.get().cancel(publisher);
            }
        }

        void publishComplete(WriteResultPublisher publisher) {
            if (publisher.changeState(this, COMPLETED)) {
                Subscriber<? super Void> s = publisher.subscriber;
                Assert.state(s != null, "No subscriber");
                s.onComplete();
            } else {
                publisher.state.get().publishComplete(publisher);
            }
        }

        void publishError(WriteResultPublisher publisher, Throwable t) {
            if (publisher.changeState(this, COMPLETED)) {
                Subscriber<? super Void> s = publisher.subscriber;
                Assert.state(s != null, "No subscriber");
                s.onError(t);
            } else {
                publisher.state.get().publishError(publisher, t);
            }
        }
    }

}
