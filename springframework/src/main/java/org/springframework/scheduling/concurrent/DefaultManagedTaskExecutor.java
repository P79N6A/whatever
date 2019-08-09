package org.springframework.scheduling.concurrent;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.jndi.JndiTemplate;
import org.springframework.lang.Nullable;

import javax.naming.NamingException;
import java.util.Properties;
import java.util.concurrent.Executor;

public class DefaultManagedTaskExecutor extends ConcurrentTaskExecutor implements InitializingBean {

    private JndiLocatorDelegate jndiLocator = new JndiLocatorDelegate();

    @Nullable
    private String jndiName = "java:comp/DefaultManagedExecutorService";

    public void setJndiTemplate(JndiTemplate jndiTemplate) {
        this.jndiLocator.setJndiTemplate(jndiTemplate);
    }

    public void setJndiEnvironment(Properties jndiEnvironment) {
        this.jndiLocator.setJndiEnvironment(jndiEnvironment);
    }

    public void setResourceRef(boolean resourceRef) {
        this.jndiLocator.setResourceRef(resourceRef);
    }

    public void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }

    @Override
    public void afterPropertiesSet() throws NamingException {
        if (this.jndiName != null) {
            setConcurrentExecutor(this.jndiLocator.lookup(this.jndiName, Executor.class));
        }
    }

}
