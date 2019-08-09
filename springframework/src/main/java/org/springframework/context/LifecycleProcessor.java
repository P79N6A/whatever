package org.springframework.context;

public interface LifecycleProcessor extends Lifecycle {

    void onRefresh();

    void onClose();

}
