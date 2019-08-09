package org.springframework.web.util;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.io.Serializable;

public class HttpSessionMutexListener implements HttpSessionListener {

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        event.getSession().setAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE, new Mutex());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        event.getSession().removeAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE);
    }

    @SuppressWarnings("serial")
    private static class Mutex implements Serializable {
    }

}
