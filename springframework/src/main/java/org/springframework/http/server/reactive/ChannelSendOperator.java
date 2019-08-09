package org.springframework.http.server.reactive;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import java.util.function.Function;

public class ChannelSendOperator<T> extends Mono<Void> implements Scannable {

    private final Function<Publisher<T>, Publisher<Void>> writeFunction;

    private final Flux<T> source;

    public ChannelSendOperator(Publisher<? extends T> source, Function<Publisher<T>, Publisher<Void>> writeFunction) {
        this.source = Flux.from(source);
        this.writeFunction = writeFunction;
    }

    @Override
    @Nullable
    @SuppressWarnings("rawtypes")
    public Object scanUnsafe(Attr key) {
        if (key == Attr.PREFETCH) {
            return Integer.MAX_VALUE;
        }
        if (key == Attr.PARENT) {
            return this.source;
        }
        return null;
    }

    @Override
    public void subscribe(CoreSubscriber<? super Void> actual) {
        this.source.subscribe(new WriteBarrier(actual));
    }

    private enum State {

        NEW,

        FIRST_SIGNAL_RECEIVED,

        EMITTING_CACHED_SIGNALS,

        READY_TO_WRITE

    }

    private class WriteBarrier implements CoreSubscriber<T>, Subscription, Publisher<T> {

        private final WriteCompletionBarrier writeCompletionBarrier;

        @Nullable
        private Subscription subscription;

        @Nullable
        private T item;

        @Nullable
        private Throwable error;

        private boolean completed = false;

        private long demandBeforeReadyToWrite;

        private State state = State.NEW;

        @Nullable
        private Subscriber<? super T> writeSubscriber;

        WriteBarrier(CoreSubscriber<? super Void> completionSubscriber) {
            this.writeCompletionBarrier = new WriteCompletionBarrier(completionSubscriber, this);
        }
        // Subscriber<T> methods (we're the subscriber to the write source)..

        @Override
        public final void onSubscribe(Subscription s) {
            if (Operators.validate(this.subscription, s)) {
                this.subscription = s;
                this.writeCompletionBarrier.connect();
                s.request(1);
            }
        }

        @Override
        public final void onNext(T item) {
            if (this.state == State.READY_TO_WRITE) {
                requiredWriteSubscriber().onNext(item);
                return;
            }
            //FIXME revisit in case of reentrant sync deadlock
            synchronized (this) {
                if (this.state == State.READY_TO_WRITE) {
                    requiredWriteSubscriber().onNext(item);
                } else if (this.state == State.NEW) {
                    this.item = item;
                    this.state = State.FIRST_SIGNAL_RECEIVED;
                    writeFunction.apply(this).subscribe(this.writeCompletionBarrier);
                } else {
                    if (this.subscription != null) {
                        this.subscription.cancel();
                    }
                    this.writeCompletionBarrier.onError(new IllegalStateException("Unexpected item."));
                }
            }
        }

        private Subscriber<? super T> requiredWriteSubscriber() {
            Assert.state(this.writeSubscriber != null, "No write subscriber");
            return this.writeSubscriber;
        }

        @Override
        public final void onError(Throwable ex) {
            if (this.state == State.READY_TO_WRITE) {
                requiredWriteSubscriber().onError(ex);
                return;
            }
            synchronized (this) {
                if (this.state == State.READY_TO_WRITE) {
                    requiredWriteSubscriber().onError(ex);
                } else if (this.state == State.NEW) {
                    this.state = State.FIRST_SIGNAL_RECEIVED;
                    this.writeCompletionBarrier.onError(ex);
                } else {
                    this.error = ex;
                }
            }
        }

        @Override
        public final void onComplete() {
            if (this.state == State.READY_TO_WRITE) {
                requiredWriteSubscriber().onComplete();
                return;
            }
            synchronized (this) {
                if (this.state == State.READY_TO_WRITE) {
                    requiredWriteSubscriber().onComplete();
                } else if (this.state == State.NEW) {
                    this.completed = true;
                    this.state = State.FIRST_SIGNAL_RECEIVED;
                    writeFunction.apply(this).subscribe(this.writeCompletionBarrier);
                } else {
                    this.completed = true;
                }
            }
        }

        @Override
        public Context currentContext() {
            return this.writeCompletionBarrier.currentContext();
        }
        // Subscription methods (we're the Subscription to the writeSubscriber)..

        @Override
        public void request(long n) {
            Subscription s = this.subscription;
            if (s == null) {
                return;
            }
            if (this.state == State.READY_TO_WRITE) {
                s.request(n);
                return;
            }
            synchronized (this) {
                if (this.writeSubscriber != null) {
                    if (this.state == State.EMITTING_CACHED_SIGNALS) {
                        this.demandBeforeReadyToWrite = n;
                        return;
                    }
                    try {
                        this.state = State.EMITTING_CACHED_SIGNALS;
                        if (emitCachedSignals()) {
                            return;
                        }
                        n = n + this.demandBeforeReadyToWrite - 1;
                        if (n == 0) {
                            return;
                        }
                    } finally {
                        this.state = State.READY_TO_WRITE;
                    }
                }
            }
            s.request(n);
        }

        private boolean emitCachedSignals() {
            if (this.error != null) {
                try {
                    requiredWriteSubscriber().onError(this.error);
                } finally {
                    releaseCachedItem();
                }
                return true;
            }
            T item = this.item;
            this.item = null;
            if (item != null) {
                requiredWriteSubscriber().onNext(item);
            }
            if (this.completed) {
                requiredWriteSubscriber().onComplete();
                return true;
            }
            return false;
        }

        @Override
        public void cancel() {
            Subscription s = this.subscription;
            if (s != null) {
                this.subscription = null;
                try {
                    s.cancel();
                } finally {
                    releaseCachedItem();
                }
            }
        }

        private void releaseCachedItem() {
            synchronized (this) {
                Object item = this.item;
                if (item instanceof DataBuffer) {
                    DataBufferUtils.release((DataBuffer) item);
                }
                this.item = null;
            }
        }
        // Publisher<T> methods (we're the Publisher to the writeSubscriber)..

        @Override
        public void subscribe(Subscriber<? super T> writeSubscriber) {
            synchronized (this) {
                Assert.state(this.writeSubscriber == null, "Only one write subscriber supported");
                this.writeSubscriber = writeSubscriber;
                if (this.error != null || this.completed) {
                    this.writeSubscriber.onSubscribe(Operators.emptySubscription());
                    emitCachedSignals();
                } else {
                    this.writeSubscriber.onSubscribe(this);
                }
            }
        }

    }

    private class WriteCompletionBarrier implements CoreSubscriber<Void>, Subscription {

        private final CoreSubscriber<? super Void> completionSubscriber;

        private final WriteBarrier writeBarrier;

        @Nullable
        private Subscription subscription;

        public WriteCompletionBarrier(CoreSubscriber<? super Void> subscriber, WriteBarrier writeBarrier) {
            this.completionSubscriber = subscriber;
            this.writeBarrier = writeBarrier;
        }

        public void connect() {
            this.completionSubscriber.onSubscribe(this);
        }
        // Subscriber methods (we're the subscriber to the write function)..

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(Void aVoid) {
        }

        @Override
        public void onError(Throwable ex) {
            try {
                this.completionSubscriber.onError(ex);
            } finally {
                this.writeBarrier.releaseCachedItem();
            }
        }

        @Override
        public void onComplete() {
            this.completionSubscriber.onComplete();
        }

        @Override
        public Context currentContext() {
            return this.completionSubscriber.currentContext();
        }

        @Override
        public void request(long n) {
            // Ignore: we don't produce data
        }

        @Override
        public void cancel() {
            this.writeBarrier.cancel();
            Subscription subscription = this.subscription;
            if (subscription != null) {
                subscription.cancel();
            }
        }

    }

}
