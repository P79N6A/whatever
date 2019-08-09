package org.springframework.http.server.reactive;

import org.apache.commons.logging.Log;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.core.log.LogDelegateFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractListenerWriteProcessor<T> implements Processor<T, Void> {

    protected static final Log rsWriteLogger = LogDelegateFactory.getHiddenLog(AbstractListenerWriteProcessor.class);

    private final AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

    @Nullable
    private Subscription subscription;

    @Nullable
    private volatile T currentData;

    private volatile boolean subscriberCompleted;

    private final WriteResultPublisher resultPublisher;

    private final String logPrefix;

    public AbstractListenerWriteProcessor() {
        this("");
    }

    public AbstractListenerWriteProcessor(String logPrefix) {
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
    public final void onNext(T data) {
        if (rsWriteLogger.isTraceEnabled()) {
            rsWriteLogger.trace(getLogPrefix() + "Item to write");
        }
        this.state.get().onNext(this, data);
    }

    @Override
    public final void onError(Throwable ex) {
        if (rsWriteLogger.isTraceEnabled()) {
            rsWriteLogger.trace(getLogPrefix() + "Write source error: " + ex);
        }
        this.state.get().onError(this, ex);
    }

    @Override
    public final void onComplete() {
        if (rsWriteLogger.isTraceEnabled()) {
            rsWriteLogger.trace(getLogPrefix() + "No more items to write");
        }
        this.state.get().onComplete(this);
    }

    public final void onWritePossible() {
        if (rsWriteLogger.isTraceEnabled()) {
            rsWriteLogger.trace(getLogPrefix() + "onWritePossible");
        }
        this.state.get().onWritePossible(this);
    }

    public void cancel() {
        rsWriteLogger.trace(getLogPrefix() + "Cancellation");
        if (this.subscription != null) {
            this.subscription.cancel();
        }
    }
    // Publisher implementation for result notifications...

    @Override
    public final void subscribe(Subscriber<? super Void> subscriber) {
        // Technically, cancellation from the result subscriber should be propagated
        // to the upstream subscription. In practice, HttpHandler server adapters
        // don't have a reason to cancel the result subscription.
        this.resultPublisher.subscribe(subscriber);
    }
    // Write API methods to be implemented or template methods to override...

    protected abstract boolean isDataEmpty(T data);

    protected void dataReceived(T data) {
        T prev = this.currentData;
        if (prev != null) {
            // This shouldn't happen:
            //   1. dataReceived can only be called from REQUESTED state
            //   2. currentData is cleared before requesting
            discardData(data);
            cancel();
            onError(new IllegalStateException("Received new data while current not processed yet."));
        }
        this.currentData = data;
    }

    protected abstract boolean isWritePossible();

    protected abstract boolean write(T data) throws IOException;

    @Deprecated
    protected void writingPaused() {
    }

    protected void writingComplete() {
    }

    protected void writingFailed(Throwable ex) {
    }

    protected abstract void discardData(T data);
    // Private methods for use from State's...

    private boolean changeState(State oldState, State newState) {
        boolean result = this.state.compareAndSet(oldState, newState);
        if (result && rsWriteLogger.isTraceEnabled()) {
            rsWriteLogger.trace(getLogPrefix() + oldState + " -> " + newState);
        }
        return result;
    }

    private void changeStateToReceived(State oldState) {
        if (changeState(oldState, State.RECEIVED)) {
            writeIfPossible();
        }
    }

    private void changeStateToComplete(State oldState) {
        if (changeState(oldState, State.COMPLETED)) {
            discardCurrentData();
            writingComplete();
            this.resultPublisher.publishComplete();
        } else {
            this.state.get().onComplete(this);
        }
    }

    private void writeIfPossible() {
        boolean result = isWritePossible();
        if (!result && rsWriteLogger.isTraceEnabled()) {
            rsWriteLogger.trace(getLogPrefix() + "isWritePossible: false");
        }
        if (result) {
            onWritePossible();
        }
    }

    private void discardCurrentData() {
        T data = this.currentData;
        this.currentData = null;
        if (data != null) {
            discardData(data);
        }
    }

    private enum State {

        UNSUBSCRIBED {
            @Override
            public <T> void onSubscribe(AbstractListenerWriteProcessor<T> processor, Subscription subscription) {
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
            public <T> void onNext(AbstractListenerWriteProcessor<T> processor, T data) {
                if (processor.isDataEmpty(data)) {
                    Assert.state(processor.subscription != null, "No subscription");
                    processor.subscription.request(1);
                } else {
                    processor.dataReceived(data);
                    processor.changeStateToReceived(this);
                }
            }

            @Override
            public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
                processor.changeStateToComplete(this);
            }
        },

        RECEIVED {
            @SuppressWarnings("deprecation")
            @Override
            public <T> void onWritePossible(AbstractListenerWriteProcessor<T> processor) {
                if (processor.changeState(this, WRITING)) {
                    T data = processor.currentData;
                    Assert.state(data != null, "No data");
                    try {
                        if (processor.write(data)) {
                            if (processor.changeState(WRITING, REQUESTED)) {
                                processor.currentData = null;
                                if (processor.subscriberCompleted) {
                                    processor.changeStateToComplete(REQUESTED);
                                } else {
                                    processor.writingPaused();
                                    Assert.state(processor.subscription != null, "No subscription");
                                    processor.subscription.request(1);
                                }
                            }
                        } else {
                            processor.changeStateToReceived(WRITING);
                        }
                    } catch (IOException ex) {
                        processor.writingFailed(ex);
                    }
                }
            }

            @Override
            public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
                processor.subscriberCompleted = true;
            }
        },

        WRITING {
            @Override
            public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
                processor.subscriberCompleted = true;
            }
        },

        COMPLETED {
            @Override
            public <T> void onNext(AbstractListenerWriteProcessor<T> processor, T data) {
                // ignore
            }

            @Override
            public <T> void onError(AbstractListenerWriteProcessor<T> processor, Throwable ex) {
                // ignore
            }

            @Override
            public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
                // ignore
            }
        };

        public <T> void onSubscribe(AbstractListenerWriteProcessor<T> processor, Subscription subscription) {
            subscription.cancel();
        }

        public <T> void onNext(AbstractListenerWriteProcessor<T> processor, T data) {
            processor.discardData(data);
            processor.cancel();
            processor.onError(new IllegalStateException("Illegal onNext without demand"));
        }

        public <T> void onError(AbstractListenerWriteProcessor<T> processor, Throwable ex) {
            if (processor.changeState(this, COMPLETED)) {
                processor.discardCurrentData();
                processor.writingComplete();
                processor.resultPublisher.publishError(ex);
            } else {
                processor.state.get().onError(processor, ex);
            }
        }

        public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
            throw new IllegalStateException(toString());
        }

        public <T> void onWritePossible(AbstractListenerWriteProcessor<T> processor) {
            // ignore
        }
    }

}
