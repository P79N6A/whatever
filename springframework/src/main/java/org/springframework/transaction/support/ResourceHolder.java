package org.springframework.transaction.support;

public interface ResourceHolder {

    void reset();

    void unbound();

    boolean isVoid();

}
