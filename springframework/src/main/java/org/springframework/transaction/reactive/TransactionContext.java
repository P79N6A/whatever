package org.springframework.transaction.reactive;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TransactionContext {

    private final @Nullable
    TransactionContext parent;

    private final UUID contextId = UUID.randomUUID();

    private final Map<Object, Object> resources = new LinkedHashMap<>();

    @Nullable
    private Set<TransactionSynchronization> synchronizations;

    private volatile @Nullable
    String currentTransactionName;

    private volatile boolean currentTransactionReadOnly;

    private volatile @Nullable
    Integer currentTransactionIsolationLevel;

    private volatile boolean actualTransactionActive;

    TransactionContext() {
        this(null);
    }

    TransactionContext(@Nullable TransactionContext parent) {
        this.parent = parent;
    }

    @Nullable
    public TransactionContext getParent() {
        return this.parent;
    }

    public String getName() {
        if (StringUtils.hasText(this.currentTransactionName)) {
            return this.contextId + ": " + this.currentTransactionName;
        }
        return this.contextId.toString();
    }

    public UUID getContextId() {
        return this.contextId;
    }

    public Map<Object, Object> getResources() {
        return this.resources;
    }

    public void setSynchronizations(@Nullable Set<TransactionSynchronization> synchronizations) {
        this.synchronizations = synchronizations;
    }

    @Nullable
    public Set<TransactionSynchronization> getSynchronizations() {
        return this.synchronizations;
    }

    public void setCurrentTransactionName(@Nullable String currentTransactionName) {
        this.currentTransactionName = currentTransactionName;
    }

    @Nullable
    public String getCurrentTransactionName() {
        return this.currentTransactionName;
    }

    public void setCurrentTransactionReadOnly(boolean currentTransactionReadOnly) {
        this.currentTransactionReadOnly = currentTransactionReadOnly;
    }

    public boolean isCurrentTransactionReadOnly() {
        return this.currentTransactionReadOnly;
    }

    public void setCurrentTransactionIsolationLevel(@Nullable Integer currentTransactionIsolationLevel) {
        this.currentTransactionIsolationLevel = currentTransactionIsolationLevel;
    }

    @Nullable
    public Integer getCurrentTransactionIsolationLevel() {
        return this.currentTransactionIsolationLevel;
    }

    public void setActualTransactionActive(boolean actualTransactionActive) {
        this.actualTransactionActive = actualTransactionActive;
    }

    public boolean isActualTransactionActive() {
        return this.actualTransactionActive;
    }

    public void clear() {
        this.synchronizations = null;
        this.currentTransactionName = null;
        this.currentTransactionReadOnly = false;
        this.currentTransactionIsolationLevel = null;
        this.actualTransactionActive = false;
    }

}
