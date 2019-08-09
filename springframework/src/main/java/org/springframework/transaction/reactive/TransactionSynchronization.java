package org.springframework.transaction.reactive;

import reactor.core.publisher.Mono;

public interface TransactionSynchronization {

    int STATUS_COMMITTED = 0;

    int STATUS_ROLLED_BACK = 1;

    int STATUS_UNKNOWN = 2;

    default Mono<Void> suspend() {
        return Mono.empty();
    }

    default Mono<Void> resume() {
        return Mono.empty();
    }

    default Mono<Void> beforeCommit(boolean readOnly) {
        return Mono.empty();
    }

    default Mono<Void> beforeCompletion() {
        return Mono.empty();
    }

    default Mono<Void> afterCommit() {
        return Mono.empty();
    }

    default Mono<Void> afterCompletion(int status) {
        return Mono.empty();
    }

}
