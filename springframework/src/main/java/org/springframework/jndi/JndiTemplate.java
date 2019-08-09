package org.springframework.jndi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import java.util.Hashtable;
import java.util.Properties;

public class JndiTemplate {

    protected final Log logger = LogFactory.getLog(getClass());

    @Nullable
    private Properties environment;

    public JndiTemplate() {
    }

    public JndiTemplate(@Nullable Properties environment) {
        this.environment = environment;
    }

    public void setEnvironment(@Nullable Properties environment) {
        this.environment = environment;
    }

    @Nullable
    public Properties getEnvironment() {
        return this.environment;
    }

    @Nullable
    public <T> T execute(JndiCallback<T> contextCallback) throws NamingException {
        Context ctx = getContext();
        try {
            return contextCallback.doInContext(ctx);
        } finally {
            releaseContext(ctx);
        }
    }

    public Context getContext() throws NamingException {
        return createInitialContext();
    }

    public void releaseContext(@Nullable Context ctx) {
        if (ctx != null) {
            try {
                ctx.close();
            } catch (NamingException ex) {
                logger.debug("Could not close JNDI InitialContext", ex);
            }
        }
    }

    protected Context createInitialContext() throws NamingException {
        Hashtable<?, ?> icEnv = null;
        Properties env = getEnvironment();
        if (env != null) {
            icEnv = new Hashtable<>(env.size());
            CollectionUtils.mergePropertiesIntoMap(env, icEnv);
        }
        return new InitialContext(icEnv);
    }

    public Object lookup(final String name) throws NamingException {
        if (logger.isDebugEnabled()) {
            logger.debug("Looking up JNDI object with name [" + name + "]");
        }
        Object result = execute(ctx -> ctx.lookup(name));
        if (result == null) {
            throw new NameNotFoundException("JNDI object with [" + name + "] not found: JNDI implementation returned null");
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T> T lookup(String name, @Nullable Class<T> requiredType) throws NamingException {
        Object jndiObject = lookup(name);
        if (requiredType != null && !requiredType.isInstance(jndiObject)) {
            throw new TypeMismatchNamingException(name, requiredType, jndiObject.getClass());
        }
        return (T) jndiObject;
    }

    public void bind(final String name, final Object object) throws NamingException {
        if (logger.isDebugEnabled()) {
            logger.debug("Binding JNDI object with name [" + name + "]");
        }
        execute(ctx -> {
            ctx.bind(name, object);
            return null;
        });
    }

    public void rebind(final String name, final Object object) throws NamingException {
        if (logger.isDebugEnabled()) {
            logger.debug("Rebinding JNDI object with name [" + name + "]");
        }
        execute(ctx -> {
            ctx.rebind(name, object);
            return null;
        });
    }

    public void unbind(final String name) throws NamingException {
        if (logger.isDebugEnabled()) {
            logger.debug("Unbinding JNDI object with name [" + name + "]");
        }
        execute(ctx -> {
            ctx.unbind(name);
            return null;
        });
    }

}
