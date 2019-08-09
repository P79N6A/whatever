package org.springframework.transaction.reactive;

import org.springframework.transaction.NoTransactionException;

import java.util.Deque;

final class TransactionContextHolder {

    private final Deque<TransactionContext> transactionStack;

    TransactionContextHolder(Deque<TransactionContext> transactionStack) {
        this.transactionStack = transactionStack;
    }

    TransactionContext currentContext() {
        TransactionContext context = this.transactionStack.peek();
        if (context == null) {
            throw new NoTransactionException("No transaction in context");
        }
        return context;
    }

    TransactionContext createContext() {
        TransactionContext context = this.transactionStack.peek();
        if (context != null) {
            context = new TransactionContext(context);
        } else {
            context = new TransactionContext();
        }
        this.transactionStack.push(context);
        return context;
    }

    boolean hasContext() {
        return !this.transactionStack.isEmpty();
    }

}
