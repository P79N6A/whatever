package org.springframework.boot.diagnostics;

import org.springframework.core.ResolvableType;

public abstract class AbstractFailureAnalyzer<T extends Throwable> implements FailureAnalyzer {

    @Override
    public FailureAnalysis analyze(Throwable failure) {
        T cause = findCause(failure, getCauseType());
        if (cause != null) {
            return analyze(failure, cause);
        }
        return null;
    }

    protected abstract FailureAnalysis analyze(Throwable rootFailure, T cause);

    @SuppressWarnings("unchecked")
    protected Class<? extends T> getCauseType() {
        return (Class<? extends T>) ResolvableType.forClass(AbstractFailureAnalyzer.class, getClass()).resolveGeneric();
    }

    @SuppressWarnings("unchecked")
    protected final <E extends Throwable> E findCause(Throwable failure, Class<E> type) {
        while (failure != null) {
            if (type.isInstance(failure)) {
                return (E) failure;
            }
            failure = failure.getCause();
        }
        return null;
    }

}
