package org.springframework.web.context.support;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource.StubPropertySource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.*;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public abstract class WebApplicationContextUtils {

    private static final boolean jsfPresent = ClassUtils.isPresent("javax.faces.context.FacesContext", RequestContextHolder.class.getClassLoader());

    public static WebApplicationContext getRequiredWebApplicationContext(ServletContext sc) throws IllegalStateException {
        WebApplicationContext wac = getWebApplicationContext(sc);
        if (wac == null) {
            throw new IllegalStateException("No WebApplicationContext found: no ContextLoaderListener registered?");
        }
        return wac;
    }

    @Nullable
    public static WebApplicationContext getWebApplicationContext(ServletContext sc) {
        return getWebApplicationContext(sc, WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
    }

    @Nullable
    public static WebApplicationContext getWebApplicationContext(ServletContext sc, String attrName) {
        Assert.notNull(sc, "ServletContext must not be null");
        Object attr = sc.getAttribute(attrName);
        if (attr == null) {
            return null;
        }
        if (attr instanceof RuntimeException) {
            throw (RuntimeException) attr;
        }
        if (attr instanceof Error) {
            throw (Error) attr;
        }
        if (attr instanceof Exception) {
            throw new IllegalStateException((Exception) attr);
        }
        if (!(attr instanceof WebApplicationContext)) {
            throw new IllegalStateException("Context attribute is not of type WebApplicationContext: " + attr);
        }
        return (WebApplicationContext) attr;
    }

    @Nullable
    public static WebApplicationContext findWebApplicationContext(ServletContext sc) {
        WebApplicationContext wac = getWebApplicationContext(sc);
        if (wac == null) {
            Enumeration<String> attrNames = sc.getAttributeNames();
            while (attrNames.hasMoreElements()) {
                String attrName = attrNames.nextElement();
                Object attrValue = sc.getAttribute(attrName);
                if (attrValue instanceof WebApplicationContext) {
                    if (wac != null) {
                        throw new IllegalStateException("No unique WebApplicationContext found: more than one " + "DispatcherServlet registered with publishContext=true?");
                    }
                    wac = (WebApplicationContext) attrValue;
                }
            }
        }
        return wac;
    }



    public static void registerWebApplicationScopes(ConfigurableListableBeanFactory beanFactory) {
        registerWebApplicationScopes(beanFactory, null);
    }

    public static void registerWebApplicationScopes(ConfigurableListableBeanFactory beanFactory, @Nullable ServletContext sc) {
        beanFactory.registerScope(WebApplicationContext.SCOPE_REQUEST, new RequestScope());
        beanFactory.registerScope(WebApplicationContext.SCOPE_SESSION, new SessionScope());
        if (sc != null) {
            ServletContextScope appScope = new ServletContextScope(sc);
            beanFactory.registerScope(WebApplicationContext.SCOPE_APPLICATION, appScope);
            // Register as ServletContext attribute, for ContextCleanupListener to detect it.
            sc.setAttribute(ServletContextScope.class.getName(), appScope);
        }
        // 注册ServletRequest的ObjectFactory，用来依赖注入ServletRequest对象
        beanFactory.registerResolvableDependency(ServletRequest.class, new RequestObjectFactory());
        // 注册ServletResponse的ObjectFactory，用来依赖注入ServletResponse对象
        beanFactory.registerResolvableDependency(ServletResponse.class, new ResponseObjectFactory());
        // 注册HttpSession的ObjectFactory，用来依赖注入HttpSession对象
        beanFactory.registerResolvableDependency(HttpSession.class, new SessionObjectFactory());
        // 注册WebRequest的ObjectFactory，用来依赖注入WebRequest对象
        beanFactory.registerResolvableDependency(WebRequest.class, new WebRequestObjectFactory());
        if (jsfPresent) {
            FacesDependencyRegistrar.registerFacesDependencies(beanFactory);
        }
    }

    public static void registerEnvironmentBeans(ConfigurableListableBeanFactory bf, @Nullable ServletContext sc) {
        registerEnvironmentBeans(bf, sc, null);
    }

    public static void registerEnvironmentBeans(ConfigurableListableBeanFactory bf, @Nullable ServletContext servletContext, @Nullable ServletConfig servletConfig) {
        if (servletContext != null && !bf.containsBean(WebApplicationContext.SERVLET_CONTEXT_BEAN_NAME)) {
            bf.registerSingleton(WebApplicationContext.SERVLET_CONTEXT_BEAN_NAME, servletContext);
        }
        if (servletConfig != null && !bf.containsBean(ConfigurableWebApplicationContext.SERVLET_CONFIG_BEAN_NAME)) {
            bf.registerSingleton(ConfigurableWebApplicationContext.SERVLET_CONFIG_BEAN_NAME, servletConfig);
        }
        if (!bf.containsBean(WebApplicationContext.CONTEXT_PARAMETERS_BEAN_NAME)) {
            Map<String, String> parameterMap = new HashMap<>();
            if (servletContext != null) {
                Enumeration<?> paramNameEnum = servletContext.getInitParameterNames();
                while (paramNameEnum.hasMoreElements()) {
                    String paramName = (String) paramNameEnum.nextElement();
                    parameterMap.put(paramName, servletContext.getInitParameter(paramName));
                }
            }
            if (servletConfig != null) {
                Enumeration<?> paramNameEnum = servletConfig.getInitParameterNames();
                while (paramNameEnum.hasMoreElements()) {
                    String paramName = (String) paramNameEnum.nextElement();
                    parameterMap.put(paramName, servletConfig.getInitParameter(paramName));
                }
            }
            bf.registerSingleton(WebApplicationContext.CONTEXT_PARAMETERS_BEAN_NAME, Collections.unmodifiableMap(parameterMap));
        }
        if (!bf.containsBean(WebApplicationContext.CONTEXT_ATTRIBUTES_BEAN_NAME)) {
            Map<String, Object> attributeMap = new HashMap<>();
            if (servletContext != null) {
                Enumeration<?> attrNameEnum = servletContext.getAttributeNames();
                while (attrNameEnum.hasMoreElements()) {
                    String attrName = (String) attrNameEnum.nextElement();
                    attributeMap.put(attrName, servletContext.getAttribute(attrName));
                }
            }
            bf.registerSingleton(WebApplicationContext.CONTEXT_ATTRIBUTES_BEAN_NAME, Collections.unmodifiableMap(attributeMap));
        }
    }

    public static void initServletPropertySources(MutablePropertySources propertySources, ServletContext servletContext) {
        initServletPropertySources(propertySources, servletContext, null);
    }

    public static void initServletPropertySources(MutablePropertySources sources, @Nullable ServletContext servletContext, @Nullable ServletConfig servletConfig) {
        Assert.notNull(sources, "'propertySources' must not be null");
        String name = StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME;
        if (servletContext != null && sources.contains(name) && sources.get(name) instanceof StubPropertySource) {
            sources.replace(name, new ServletContextPropertySource(name, servletContext));
        }
        name = StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME;
        if (servletConfig != null && sources.contains(name) && sources.get(name) instanceof StubPropertySource) {
            sources.replace(name, new ServletConfigPropertySource(name, servletConfig));
        }
    }

    private static ServletRequestAttributes currentRequestAttributes() {
        // 从当前请求处理线程获取ServletRequestAttributes对象，进而获取ServletRequest，ServletResponse，HttpSession，WebRequest对象
        RequestAttributes requestAttr = RequestContextHolder.currentRequestAttributes();
        if (!(requestAttr instanceof ServletRequestAttributes)) {
            throw new IllegalStateException("Current request is not a servlet request");
        }
        return (ServletRequestAttributes) requestAttr;
    }

    //////////// 四个私有嵌套静态内部类 ///////////

    @SuppressWarnings("serial")
    private static class RequestObjectFactory implements ObjectFactory<ServletRequest>, Serializable {

        @Override
        public ServletRequest getObject() {
            return currentRequestAttributes().getRequest();
        }

        @Override
        public String toString() {
            return "Current HttpServletRequest";
        }

    }

    @SuppressWarnings("serial")
    private static class ResponseObjectFactory implements ObjectFactory<ServletResponse>, Serializable {

        @Override
        public ServletResponse getObject() {
            ServletResponse response = currentRequestAttributes().getResponse();
            if (response == null) {
                throw new IllegalStateException("Current servlet response not available - " + "consider using RequestContextFilter instead of RequestContextListener");
            }
            return response;
        }

        @Override
        public String toString() {
            return "Current HttpServletResponse";
        }

    }

    @SuppressWarnings("serial")
    private static class SessionObjectFactory implements ObjectFactory<HttpSession>, Serializable {

        @Override
        public HttpSession getObject() {
            return currentRequestAttributes().getRequest().getSession();
        }

        @Override
        public String toString() {
            return "Current HttpSession";
        }

    }

    @SuppressWarnings("serial")
    private static class WebRequestObjectFactory implements ObjectFactory<WebRequest>, Serializable {

        @Override
        public WebRequest getObject() {
            ServletRequestAttributes requestAttr = currentRequestAttributes();
            return new ServletWebRequest(requestAttr.getRequest(), requestAttr.getResponse());
        }

        @Override
        public String toString() {
            return "Current ServletWebRequest";
        }

    }

    private static class FacesDependencyRegistrar {

        public static void registerFacesDependencies(ConfigurableListableBeanFactory beanFactory) {
            beanFactory.registerResolvableDependency(FacesContext.class, new ObjectFactory<FacesContext>() {
                @Override
                public FacesContext getObject() {
                    return FacesContext.getCurrentInstance();
                }

                @Override
                public String toString() {
                    return "Current JSF FacesContext";
                }
            });
            beanFactory.registerResolvableDependency(ExternalContext.class, new ObjectFactory<ExternalContext>() {
                @Override
                public ExternalContext getObject() {
                    return FacesContext.getCurrentInstance().getExternalContext();
                }

                @Override
                public String toString() {
                    return "Current JSF ExternalContext";
                }
            });
        }

    }

}
