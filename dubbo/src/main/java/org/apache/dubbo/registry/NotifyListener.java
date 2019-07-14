package org.apache.dubbo.registry;

import org.apache.dubbo.common.URL;

import java.util.List;

public interface NotifyListener {

    void notify(List<URL> urls);

}