package org.springframework.http.server.reactive;

import org.apache.commons.logging.Log;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.core.log.LogDelegateFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractListenerWriteFlushProcessor<T> implements Processor<Publisher<? extends T>, Void> {

    protected static final Log rsWriteFlushLogger = LogDelegateFactory.getHiddenLog(AbstractListenerWriteFlushProcessor.class);

    private final AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

    @Nullable
    private Subscription subscription;

    private volatile boolean subscriberCompleted;

    private final WriteResultPublisher resultPublisher;

    private final String logPrefix;

    public AbstractListenerWriteFlushProcessor() {
        this("");
    }

    public AbstractListenerWriteFlushProcessor(String logPrefix) {
        this.logPrefix = logPrefix;
        this.resultPublisher = new WriteResultPublisher(logPrefix);
    }

    public String getLogPrefix() {
        return this.logPrefix;
    }
    // Subscriber methods and async I/O notification methods...

    @Override
    public final void onSubscribe(Subscription subscription) {
        this.state.get().onSubscribe(this, subscription);
    }

    @Override
    public final void onNext(Publisher<? extends T> publisher) {
        if (rsWriteFlushLogger.isTraceEnabled()) {
            rsWriteFlushLogger.trace(getLogPrefix() + "Received onNext publisher");
        }
        this.state.get().onNext(this, publisher);
    }

    @Override
    public final void onError(Throwable ex) {
        if (rsWriteFlushLogger.isTraceEnabled()) {
            rsWriteFlushLogger.trace(getLogPrefix() + "Received onError: " + ex);
        }
        this.state.get().onError(this, ex);
    }

    @Override
    public final void onComplete() {
        if (rsWriteFlushLogger.isTraceEnabled()) {
            rsWriteFlushLogger.trace(getLogPrefix() + "Received onComplete");
        }
        this.state.get().onComplete(this);
    }

    protected final void onFlushPossible() {
        this.state.get().onFlushPossible(this);
    }

    protected void cancel() {
        if (rsWriteFlushLogger.isTraceEnabled()) {
            rsWriteFlushLogger.trace(getLogPrefix() + "Received request to cancel");
        }
        if (this.subscription != null) {
            this.subscription.cancel();
        }
    }
    // Publisher implementation for result notifications...

    @Override
    public final void subscribe(Subscriber<? super Void> subscriber) {
        this.resultPublisher.subscribe(subscriber);
    }
    // Write API methods to be implemented or template methods to override...

    protected abstract Processor<? super T, Void> createWriteProcessor();

    protected abstract boolean isWritePossible();

    protected abstract void flush() throws IOException;

    protected abstract boolean isFlushPending();

    protected void flushingFailed(Throwable t) {
    }
    // Private methods for use in State...

    private boolean changeState(State oldState, State newState) {
        boolean result = this.state.compareAndSet(oldState, newState);
        if (result && rsWriteFlushLogger.isTraceEnabled()) {
            rsWriteFlushLogger.trace(getLogPrefix() + oldState + " -> " + newState);
        }
        return result;
    }

    private void flushIfPossible() {
        boolean result = isWritePossible();
        if (rsWriteFlushLogger.isTraceEnabled()) {
            rsWriteFlushLogger.trace(getLogPrefix() + "isWritePossible[" + result + "]");
        }
        if (result) {
            onFlushPossible();
        }
    }

    private enum State {

        UNSUBSCRIBED {
            @Override
            public <T> void onSubscribe(AbstractListenerWriteFlushProcessor<T> processor, Subscription subscription) {
                Assert.notNull(subscription, "Subscription must not be null");
                if (processor.changeState(this, REQUESTED)) {
                    processor.subscription = subscription;
                    subscription.request(1);
                } else {
                    super.onSubscribe(processor, subscription);
                }
            }
        },

        REQUESTED {
            @Override
            public <T> void onNext(AbstractListenerWriteFlushProcessor<T> processor, Publisher<? extends T> currentPublisher) {
                if (processor.changeState(this, RECEIVED)) {
                    Processor<? super T, Void> currentProcessor = processor.createWriteProcessor();
                    currentPublisher.subscribe(currentProcessor);
                    currentProcessor.subscribe(new WriteResultSubscriber(processor));
                }
            }

            @Override
            public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
                if (processor.changeState(this, COMPLETED)) {
                    processor.resultPublisher.publishComplete();
                } else {
                    processor.state.get().onComplete(processor);
                }
            }
        },

        RECEIVED {
            @Override
            public <T> void writeComplete(AbstractListenerWriteFlushProcessor<T> processor) {
                try {
                    processor.flush();
                } catch (Throwable ex) {
                    processor.flushingFailed(ex);
                    return;
                }
                if (processor.changeState(this, REQUESTED)) {
                    if (processor.subscriberCompleted) {
                        if (processor.isFlushPending()) {
                            // Ensure the final flush
                            processor.changeState(REQUESTED, FLUSHING);
                            processor.flushIfPossible();
                        } else if (processor.changeState(REQUESTED, COMPLETED)) {
                            processor.resultPublisher.publishComplete();
                        } else {
                            processor.state.get().onComplete(processor);
                        }
                    } else {
                        Assert.state(processor.subscription != null, "No subscription");
                        processor.subscription.request(1);
                    }
                }
            }

            @Override
            public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
                processor.subscriberCompleted = true;
            }
        },

        FLUSHING {
            @Override
            public <T> void onFlushPossible(AbstractListenerWriteFlushProcessor<T> processor) {
                try {
                    processor.flush();
                } catch (Throwable ex) {
                    processor.flushingFailed(ex);
                    return;
                }
                if (processor.changeState(this, COMPLETED)) {
                    processor.resultPublisher.publishComplete();
                } else {
                    processor.state.get().onComplete(processor);
                }
            }

            @Override
            public <T> void onNext(AbstractListenerWriteFlushProcessor<T> proc, Publisher<? extends T> pub) {
                // ignore
            }

            @Override
            public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
                // ignore
            }
        },

        COMPLETED {
            @Override
            public <T> void onNext(AbstractListenerWriteFlushProcessor<T> proc, Publisher<? extends T> pub) {
                // ignore
            }

            @Override
            public <T> void onError(AbstractListenerWriteFlushProcessor<T> processor, Throwable t) {
                // ignore
            }

            @Override
            public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
                // ignore
            }
        };

        public <T> void onSubscribe(AbstractListenerWriteFlushProcessor<T> proc, Subscription subscription) {
            subscription.cancel();
        }

        public <T> void onNext(AbstractListenerWriteFlushProcessor<T> proc, Publisher<? extends T> pub) {
            throw new IllegalStateException(toString());
        }

        public <T> void onError(AbstractListenerWriteFlushProcessor<T> processor, Throwable ex) {
            if (processor.changeState(this, COMPLETED)) {
                processor.resultPublisher.publishError(ex);
            } else {
                processor.state.get().onError(processor, ex);
            }
        }

        public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
            throw new IllegalStateException(toString());
        }

        public <T> void writeComplete(AbstractListenerWriteFlushProcessor<T> processor) {
            throw new IllegalStateException(toString());
        }

        public <T> void onFlushPossible(AbstractListenerWriteFlushProcessor<T> processor) {
            // ignore
        }

        private static class WriteResultSubscriber implements Subscriber<Void> {

            private final AbstractListenerWriteFlushProcessor<?> processor;

            public WriteResultSubscriber(AbstractListenerWriteFlushProcessor<?> processor) {
                this.processor = processor;
            }

            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Void aVoid) {
            }

            @Override
            public void onError(Throwable ex) {
                this.processor.cancel();
                this.processor.onError(ex);
            }

            @Override
            public void onComplete() {
                if (rsWriteFlushLogger.isTraceEnabled()) {
                    rsWriteFlushLogger.trace(this.processor.getLogPrefix() + this.processor.state + " writeComplete");
                }
                this.processor.state.get().writeComplete(this.processor);
            }

        }
    }

}
