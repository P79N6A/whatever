package org.springframework.web.server.adapter;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServletHttpHandlerAdapter;
import org.springframework.util.Assert;
import org.springframework.web.WebApplicationInitializer;

import javax.servlet.*;

public abstract class AbstractReactiveWebInitializer implements WebApplicationInitializer {

    public static final String DEFAULT_SERVLET_NAME = "http-handler-adapter";

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        String servletName = getServletName();
        Assert.hasLength(servletName, "getServletName() must not return null or empty");
        ApplicationContext applicationContext = createApplicationContext();
        Assert.notNull(applicationContext, "createApplicationContext() must not return null");
        refreshApplicationContext(applicationContext);
        registerCloseListener(servletContext, applicationContext);
        HttpHandler httpHandler = WebHttpHandlerBuilder.applicationContext(applicationContext).build();
        ServletHttpHandlerAdapter servlet = new ServletHttpHandlerAdapter(httpHandler);
        ServletRegistration.Dynamic registration = servletContext.addServlet(servletName, servlet);
        if (registration == null) {
            throw new IllegalStateException("Failed to register servlet with name '" + servletName + "'. " + "Check if there is another servlet registered under the same name.");
        }
        registration.setLoadOnStartup(1);
        registration.addMapping(getServletMapping());
        registration.setAsyncSupported(true);
    }

    protected String getServletName() {
        return DEFAULT_SERVLET_NAME;
    }

    protected ApplicationContext createApplicationContext() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        Class<?>[] configClasses = getConfigClasses();
        Assert.notEmpty(configClasses, "No Spring configuration provided through getConfigClasses()");
        context.register(configClasses);
        return context;
    }

    protected abstract Class<?>[] getConfigClasses();

    protected void refreshApplicationContext(ApplicationContext context) {
        if (context instanceof ConfigurableApplicationContext) {
            ConfigurableApplicationContext cac = (ConfigurableApplicationContext) context;
            if (!cac.isActive()) {
                cac.refresh();
            }
        }
    }

    protected void registerCloseListener(ServletContext servletContext, ApplicationContext applicationContext) {
        if (applicationContext instanceof ConfigurableApplicationContext) {
            ConfigurableApplicationContext cac = (ConfigurableApplicationContext) applicationContext;
            ServletContextDestroyedListener listener = new ServletContextDestroyedListener(cac);
            servletContext.addListener(listener);
        }
    }

    protected String getServletMapping() {
        return "/";
    }

    private static class ServletContextDestroyedListener implements ServletContextListener {

        private final ConfigurableApplicationContext applicationContext;

        public ServletContextDestroyedListener(ConfigurableApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }

        @Override
        public void contextInitialized(ServletContextEvent sce) {
        }

        @Override
        public void contextDestroyed(ServletContextEvent sce) {
            this.applicationContext.close();
        }

    }

}
