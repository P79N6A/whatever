package org.springframework.jndi;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.naming.NamingException;

public abstract class JndiObjectLocator extends JndiLocatorSupport implements InitializingBean {

    @Nullable
    private String jndiName;

    @Nullable
    private Class<?> expectedType;

    public void setJndiName(@Nullable String jndiName) {
        this.jndiName = jndiName;
    }

    @Nullable
    public String getJndiName() {
        return this.jndiName;
    }

    public void setExpectedType(@Nullable Class<?> expectedType) {
        this.expectedType = expectedType;
    }

    @Nullable
    public Class<?> getExpectedType() {
        return this.expectedType;
    }

    @Override
    public void afterPropertiesSet() throws IllegalArgumentException, NamingException {
        if (!StringUtils.hasLength(getJndiName())) {
            throw new IllegalArgumentException("Property 'jndiName' is required");
        }
    }

    protected Object lookup() throws NamingException {
        String jndiName = getJndiName();
        Assert.state(jndiName != null, "No JNDI name specified");
        return lookup(jndiName, getExpectedType());
    }

}
