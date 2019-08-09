package org.springframework.web.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.lang.Nullable;
import org.springframework.web.WebApplicationInitializer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public abstract class AbstractContextLoaderInitializer implements WebApplicationInitializer {

    protected final Log logger = LogFactory.getLog(getClass());

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        registerContextLoaderListener(servletContext);
    }

    protected void registerContextLoaderListener(ServletContext servletContext) {
        WebApplicationContext rootAppContext = createRootApplicationContext();
        if (rootAppContext != null) {
            ContextLoaderListener listener = new ContextLoaderListener(rootAppContext);
            listener.setContextInitializers(getRootApplicationContextInitializers());
            servletContext.addListener(listener);
        } else {
            logger.debug("No ContextLoaderListener registered, as " + "createRootApplicationContext() did not return an application context");
        }
    }

    @Nullable
    protected abstract WebApplicationContext createRootApplicationContext();

    @Nullable
    protected ApplicationContextInitializer<?>[] getRootApplicationContextInitializers() {
        return null;
    }

}
