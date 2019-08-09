package org.springframework.transaction.reactive;

import org.springframework.transaction.NoTransactionException;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.ArrayDeque;
import java.util.function.Function;

public abstract class TransactionContextManager {

    private TransactionContextManager() {
    }

    public static Mono<TransactionContext> currentContext() throws NoTransactionException {
        return Mono.subscriberContext().handle((ctx, sink) -> {
            if (ctx.hasKey(TransactionContext.class)) {
                sink.next(ctx.get(TransactionContext.class));
                return;
            }
            if (ctx.hasKey(TransactionContextHolder.class)) {
                TransactionContextHolder holder = ctx.get(TransactionContextHolder.class);
                if (holder.hasContext()) {
                    sink.next(holder.currentContext());
                    return;
                }
            }
            sink.error(new NoTransactionException("No transaction in context"));
        });
    }

    public static Function<Context, Context> createTransactionContext() {
        return context -> context.put(TransactionContext.class, new TransactionContext());
    }

    public static Function<Context, Context> getOrCreateContext() {
        return context -> {
            TransactionContextHolder holder = context.get(TransactionContextHolder.class);
            if (holder.hasContext()) {
                return context.put(TransactionContext.class, holder.currentContext());
            }
            return context.put(TransactionContext.class, holder.createContext());
        };
    }

    public static Function<Context, Context> getOrCreateContextHolder() {
        return context -> {
            if (!context.hasKey(TransactionContextHolder.class)) {
                return context.put(TransactionContextHolder.class, new TransactionContextHolder(new ArrayDeque<>()));
            }
            return context;
        };
    }

}
