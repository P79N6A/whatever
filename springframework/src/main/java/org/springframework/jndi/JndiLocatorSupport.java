package org.springframework.jndi;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.naming.NamingException;

public abstract class JndiLocatorSupport extends JndiAccessor {

    public static final String CONTAINER_PREFIX = "java:comp/env/";

    private boolean resourceRef = false;

    public void setResourceRef(boolean resourceRef) {
        this.resourceRef = resourceRef;
    }

    public boolean isResourceRef() {
        return this.resourceRef;
    }

    protected Object lookup(String jndiName) throws NamingException {
        return lookup(jndiName, null);
    }

    protected <T> T lookup(String jndiName, @Nullable Class<T> requiredType) throws NamingException {
        Assert.notNull(jndiName, "'jndiName' must not be null");
        String convertedName = convertJndiName(jndiName);
        T jndiObject;
        try {
            jndiObject = getJndiTemplate().lookup(convertedName, requiredType);
        } catch (NamingException ex) {
            if (!convertedName.equals(jndiName)) {
                // Try fallback to originally specified name...
                if (logger.isDebugEnabled()) {
                    logger.debug("Converted JNDI name [" + convertedName + "] not found - trying original name [" + jndiName + "]. " + ex);
                }
                jndiObject = getJndiTemplate().lookup(jndiName, requiredType);
            } else {
                throw ex;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Located object with JNDI name [" + convertedName + "]");
        }
        return jndiObject;
    }

    protected String convertJndiName(String jndiName) {
        // Prepend container prefix if not already specified and no other scheme given.
        if (isResourceRef() && !jndiName.startsWith(CONTAINER_PREFIX) && jndiName.indexOf(':') == -1) {
            jndiName = CONTAINER_PREFIX + jndiName;
        }
        return jndiName;
    }

}
