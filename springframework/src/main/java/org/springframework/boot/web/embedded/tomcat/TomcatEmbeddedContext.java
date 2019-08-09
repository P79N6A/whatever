package org.springframework.boot.web.embedded.tomcat;

import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Manager;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.session.ManagerBase;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.util.ClassUtils;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

class TomcatEmbeddedContext extends StandardContext {

    private TomcatStarter starter;

    @Override
    public boolean loadOnStartup(Container[] children) {
        // deferred until later (see deferredLoadOnStartup)
        return true;
    }

    @Override
    public void setManager(Manager manager) {
        if (manager instanceof ManagerBase) {
            manager.setSessionIdGenerator(new LazySessionIdGenerator());
        }
        super.setManager(manager);
    }

    public void deferredLoadOnStartup() throws LifecycleException {
        doWithThreadContextClassLoader(getLoader().getClassLoader(), () -> getLoadOnStartupWrappers(findChildren()).forEach(this::load));
    }

    private Stream<Wrapper> getLoadOnStartupWrappers(Container[] children) {
        Map<Integer, List<Wrapper>> grouped = new TreeMap<>();
        for (Container child : children) {
            Wrapper wrapper = (Wrapper) child;
            int order = wrapper.getLoadOnStartup();
            if (order >= 0) {
                grouped.computeIfAbsent(order, ArrayList::new);
                grouped.get(order).add(wrapper);
            }
        }
        return grouped.values().stream().flatMap(List::stream);
    }

    private void load(Wrapper wrapper) {
        try {
            wrapper.load();
        } catch (ServletException ex) {
            String message = sm.getString("standardContext.loadOnStartup.loadException", getName(), wrapper.getName());
            if (getComputedFailCtxIfServletStartFails()) {
                throw new WebServerException(message, ex);
            }
            getLogger().error(message, StandardWrapper.getRootCause(ex));
        }
    }

    private void doWithThreadContextClassLoader(ClassLoader classLoader, Runnable code) {
        ClassLoader existingLoader = (classLoader != null) ? ClassUtils.overrideThreadContextClassLoader(classLoader) : null;
        try {
            code.run();
        } finally {
            if (existingLoader != null) {
                ClassUtils.overrideThreadContextClassLoader(existingLoader);
            }
        }
    }

    public void setStarter(TomcatStarter starter) {
        this.starter = starter;
    }

    public TomcatStarter getStarter() {
        return this.starter;
    }

}
