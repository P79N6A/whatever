package org.springframework.web.context.request;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.NumberUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServletRequestAttributes extends AbstractRequestAttributes {

    public static final String DESTRUCTION_CALLBACK_NAME_PREFIX = ServletRequestAttributes.class.getName() + ".DESTRUCTION_CALLBACK.";

    protected static final Set<Class<?>> immutableValueTypes = new HashSet<>(16);

    static {
        immutableValueTypes.addAll(NumberUtils.STANDARD_NUMBER_TYPES);
        immutableValueTypes.add(Boolean.class);
        immutableValueTypes.add(Character.class);
        immutableValueTypes.add(String.class);
    }

    private final HttpServletRequest request;

    @Nullable
    private HttpServletResponse response;

    @Nullable
    private volatile HttpSession session;

    private final Map<String, Object> sessionAttributesToUpdate = new ConcurrentHashMap<>(1);

    public ServletRequestAttributes(HttpServletRequest request) {
        Assert.notNull(request, "Request must not be null");
        this.request = request;
    }

    public ServletRequestAttributes(HttpServletRequest request, @Nullable HttpServletResponse response) {
        this(request);
        this.response = response;
    }

    public final HttpServletRequest getRequest() {
        return this.request;
    }

    @Nullable
    public final HttpServletResponse getResponse() {
        return this.response;
    }

    @Nullable
    protected final HttpSession getSession(boolean allowCreate) {
        if (isRequestActive()) {
            HttpSession session = this.request.getSession(allowCreate);
            this.session = session;
            return session;
        } else {
            // Access through stored session reference, if any...
            HttpSession session = this.session;
            if (session == null) {
                if (allowCreate) {
                    throw new IllegalStateException("No session found and request already completed - cannot create new session!");
                } else {
                    session = this.request.getSession(false);
                    this.session = session;
                }
            }
            return session;
        }
    }

    private HttpSession obtainSession() {
        HttpSession session = getSession(true);
        Assert.state(session != null, "No HttpSession");
        return session;
    }

    @Override
    public Object getAttribute(String name, int scope) {
        if (scope == SCOPE_REQUEST) {
            if (!isRequestActive()) {
                throw new IllegalStateException("Cannot ask for request attribute - request is not active anymore!");
            }
            return this.request.getAttribute(name);
        } else {
            HttpSession session = getSession(false);
            if (session != null) {
                try {
                    Object value = session.getAttribute(name);
                    if (value != null) {
                        this.sessionAttributesToUpdate.put(name, value);
                    }
                    return value;
                } catch (IllegalStateException ex) {
                    // Session invalidated - shouldn't usually happen.
                }
            }
            return null;
        }
    }

    @Override
    public void setAttribute(String name, Object value, int scope) {
        if (scope == SCOPE_REQUEST) {
            if (!isRequestActive()) {
                throw new IllegalStateException("Cannot set request attribute - request is not active anymore!");
            }
            this.request.setAttribute(name, value);
        } else {
            HttpSession session = obtainSession();
            this.sessionAttributesToUpdate.remove(name);
            session.setAttribute(name, value);
        }
    }

    @Override
    public void removeAttribute(String name, int scope) {
        if (scope == SCOPE_REQUEST) {
            if (isRequestActive()) {
                this.request.removeAttribute(name);
                removeRequestDestructionCallback(name);
            }
        } else {
            HttpSession session = getSession(false);
            if (session != null) {
                this.sessionAttributesToUpdate.remove(name);
                try {
                    session.removeAttribute(name);
                    // Remove any registered destruction callback as well.
                    session.removeAttribute(DESTRUCTION_CALLBACK_NAME_PREFIX + name);
                } catch (IllegalStateException ex) {
                    // Session invalidated - shouldn't usually happen.
                }
            }
        }
    }

    @Override
    public String[] getAttributeNames(int scope) {
        if (scope == SCOPE_REQUEST) {
            if (!isRequestActive()) {
                throw new IllegalStateException("Cannot ask for request attributes - request is not active anymore!");
            }
            return StringUtils.toStringArray(this.request.getAttributeNames());
        } else {
            HttpSession session = getSession(false);
            if (session != null) {
                try {
                    return StringUtils.toStringArray(session.getAttributeNames());
                } catch (IllegalStateException ex) {
                    // Session invalidated - shouldn't usually happen.
                }
            }
            return new String[0];
        }
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback, int scope) {
        if (scope == SCOPE_REQUEST) {
            registerRequestDestructionCallback(name, callback);
        } else {
            registerSessionDestructionCallback(name, callback);
        }
    }

    @Override
    public Object resolveReference(String key) {
        if (REFERENCE_REQUEST.equals(key)) {
            return this.request;
        } else if (REFERENCE_SESSION.equals(key)) {
            return getSession(true);
        } else {
            return null;
        }
    }

    @Override
    public String getSessionId() {
        return obtainSession().getId();
    }

    @Override
    public Object getSessionMutex() {
        return WebUtils.getSessionMutex(obtainSession());
    }

    @Override
    protected void updateAccessedSessionAttributes() {
        if (!this.sessionAttributesToUpdate.isEmpty()) {
            // Update all affected session attributes.
            HttpSession session = getSession(false);
            if (session != null) {
                try {
                    for (Map.Entry<String, Object> entry : this.sessionAttributesToUpdate.entrySet()) {
                        String name = entry.getKey();
                        Object newValue = entry.getValue();
                        Object oldValue = session.getAttribute(name);
                        if (oldValue == newValue && !isImmutableSessionAttribute(name, newValue)) {
                            session.setAttribute(name, newValue);
                        }
                    }
                } catch (IllegalStateException ex) {
                    // Session invalidated - shouldn't usually happen.
                }
            }
            this.sessionAttributesToUpdate.clear();
        }
    }

    protected boolean isImmutableSessionAttribute(String name, @Nullable Object value) {
        return (value == null || immutableValueTypes.contains(value.getClass()));
    }

    protected void registerSessionDestructionCallback(String name, Runnable callback) {
        HttpSession session = obtainSession();
        session.setAttribute(DESTRUCTION_CALLBACK_NAME_PREFIX + name, new DestructionCallbackBindingListener(callback));
    }

    @Override
    public String toString() {
        return this.request.toString();
    }

}
