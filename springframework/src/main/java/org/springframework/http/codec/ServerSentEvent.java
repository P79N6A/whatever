package org.springframework.http.codec;

import org.springframework.lang.Nullable;

import java.time.Duration;

public final class ServerSentEvent<T> {

    @Nullable
    private final String id;

    @Nullable
    private final String event;

    @Nullable
    private final Duration retry;

    @Nullable
    private final String comment;

    @Nullable
    private final T data;

    private ServerSentEvent(@Nullable String id, @Nullable String event, @Nullable Duration retry, @Nullable String comment, @Nullable T data) {
        this.id = id;
        this.event = event;
        this.retry = retry;
        this.comment = comment;
        this.data = data;
    }

    @Nullable
    public String id() {
        return this.id;
    }

    @Nullable
    public String event() {
        return this.event;
    }

    @Nullable
    public Duration retry() {
        return this.retry;
    }

    @Nullable
    public String comment() {
        return this.comment;
    }

    @Nullable
    public T data() {
        return this.data;
    }

    @Override
    public String toString() {
        return ("ServerSentEvent [id = '" + this.id + "\', event='" + this.event + "\', retry=" + this.retry + ", comment='" + this.comment + "', data=" + this.data + ']');
    }

    public static <T> Builder<T> builder() {
        return new BuilderImpl<>();
    }

    public static <T> Builder<T> builder(T data) {
        return new BuilderImpl<>(data);
    }

    public interface Builder<T> {

        Builder<T> id(String id);

        Builder<T> event(String event);

        Builder<T> retry(Duration retry);

        Builder<T> comment(String comment);

        Builder<T> data(@Nullable T data);

        ServerSentEvent<T> build();

    }

    private static class BuilderImpl<T> implements Builder<T> {

        @Nullable
        private String id;

        @Nullable
        private String event;

        @Nullable
        private Duration retry;

        @Nullable
        private String comment;

        @Nullable
        private T data;

        public BuilderImpl() {
        }

        public BuilderImpl(T data) {
            this.data = data;
        }

        @Override
        public Builder<T> id(String id) {
            this.id = id;
            return this;
        }

        @Override
        public Builder<T> event(String event) {
            this.event = event;
            return this;
        }

        @Override
        public Builder<T> retry(Duration retry) {
            this.retry = retry;
            return this;
        }

        @Override
        public Builder<T> comment(String comment) {
            this.comment = comment;
            return this;
        }

        @Override
        public Builder<T> data(@Nullable T data) {
            this.data = data;
            return this;
        }

        @Override
        public ServerSentEvent<T> build() {
            return new ServerSentEvent<>(this.id, this.event, this.retry, this.comment, this.data);
        }

    }

}
