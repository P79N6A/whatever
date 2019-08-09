package org.springframework.web.servlet.mvc.method.annotation;

import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.EmptyTargetSource;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.SpringObjenesis;
import org.springframework.util.*;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver;
import org.springframework.web.method.support.CompositeUriComponentsContributor;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.*;

public class MvcUriComponentsBuilder {

    public static final String MVC_URI_COMPONENTS_CONTRIBUTOR_BEAN_NAME = "mvcUriComponentsContributor";

    private static final Log logger = LogFactory.getLog(MvcUriComponentsBuilder.class);

    private static final SpringObjenesis objenesis = new SpringObjenesis();

    private static final PathMatcher pathMatcher = new AntPathMatcher();

    private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private static final CompositeUriComponentsContributor defaultUriComponentsContributor;

    static {
        defaultUriComponentsContributor = new CompositeUriComponentsContributor(new PathVariableMethodArgumentResolver(), new RequestParamMethodArgumentResolver(false));
    }

    private final UriComponentsBuilder baseUrl;

    protected MvcUriComponentsBuilder(UriComponentsBuilder baseUrl) {
        Assert.notNull(baseUrl, "'baseUrl' is required");
        this.baseUrl = baseUrl;
    }

    public static MvcUriComponentsBuilder relativeTo(UriComponentsBuilder baseUrl) {
        return new MvcUriComponentsBuilder(baseUrl);
    }

    public static UriComponentsBuilder fromController(Class<?> controllerType) {
        return fromController(null, controllerType);
    }

    public static UriComponentsBuilder fromController(@Nullable UriComponentsBuilder builder, Class<?> controllerType) {
        builder = getBaseUrlToUse(builder);
        // Externally configured prefix via PathConfigurer..
        String prefix = getPathPrefix(controllerType);
        builder.path(prefix);
        String mapping = getClassMapping(controllerType);
        builder.path(mapping);
        return builder;
    }

    public static UriComponentsBuilder fromMethodName(Class<?> controllerType, String methodName, Object... args) {
        Method method = getMethod(controllerType, methodName, args);
        return fromMethodInternal(null, controllerType, method, args);
    }

    public static UriComponentsBuilder fromMethodName(UriComponentsBuilder builder, Class<?> controllerType, String methodName, Object... args) {
        Method method = getMethod(controllerType, methodName, args);
        return fromMethodInternal(builder, controllerType, method, args);
    }

    public static UriComponentsBuilder fromMethod(Class<?> controllerType, Method method, Object... args) {
        return fromMethodInternal(null, controllerType, method, args);
    }

    public static UriComponentsBuilder fromMethod(UriComponentsBuilder baseUrl, @Nullable Class<?> controllerType, Method method, Object... args) {
        return fromMethodInternal(baseUrl, (controllerType != null ? controllerType : method.getDeclaringClass()), method, args);
    }

    public static UriComponentsBuilder fromMethodCall(Object info) {
        Assert.isInstanceOf(MethodInvocationInfo.class, info, "MethodInvocationInfo required");
        MethodInvocationInfo invocationInfo = (MethodInvocationInfo) info;
        Class<?> controllerType = invocationInfo.getControllerType();
        Method method = invocationInfo.getControllerMethod();
        Object[] arguments = invocationInfo.getArgumentValues();
        return fromMethodInternal(null, controllerType, method, arguments);
    }

    public static UriComponentsBuilder fromMethodCall(UriComponentsBuilder builder, Object info) {
        Assert.isInstanceOf(MethodInvocationInfo.class, info, "MethodInvocationInfo required");
        MethodInvocationInfo invocationInfo = (MethodInvocationInfo) info;
        Class<?> controllerType = invocationInfo.getControllerType();
        Method method = invocationInfo.getControllerMethod();
        Object[] arguments = invocationInfo.getArgumentValues();
        return fromMethodInternal(builder, controllerType, method, arguments);
    }

    public static <T> T on(Class<T> controllerType) {
        return controller(controllerType);
    }

    public static <T> T controller(Class<T> controllerType) {
        Assert.notNull(controllerType, "'controllerType' must not be null");
        return ControllerMethodInvocationInterceptor.initProxy(controllerType, null);
    }

    public static MethodArgumentBuilder fromMappingName(String mappingName) {
        return fromMappingName(null, mappingName);
    }

    public static MethodArgumentBuilder fromMappingName(@Nullable UriComponentsBuilder builder, String name) {
        WebApplicationContext wac = getWebApplicationContext();
        Assert.notNull(wac, "No WebApplicationContext. ");
        Map<String, RequestMappingInfoHandlerMapping> map = wac.getBeansOfType(RequestMappingInfoHandlerMapping.class);
        List<HandlerMethod> handlerMethods = null;
        for (RequestMappingInfoHandlerMapping mapping : map.values()) {
            handlerMethods = mapping.getHandlerMethodsForMappingName(name);
            if (handlerMethods != null) {
                break;
            }
        }
        if (handlerMethods == null) {
            throw new IllegalArgumentException("Mapping not found: " + name);
        } else if (handlerMethods.size() != 1) {
            throw new IllegalArgumentException("No unique match for mapping " + name + ": " + handlerMethods);
        } else {
            HandlerMethod handlerMethod = handlerMethods.get(0);
            Class<?> controllerType = handlerMethod.getBeanType();
            Method method = handlerMethod.getMethod();
            return new MethodArgumentBuilder(builder, controllerType, method);
        }
    }
    // Instance methods, relative to a base UriComponentsBuilder...

    public UriComponentsBuilder withController(Class<?> controllerType) {
        return fromController(this.baseUrl, controllerType);
    }

    public UriComponentsBuilder withMethodName(Class<?> controllerType, String methodName, Object... args) {
        return fromMethodName(this.baseUrl, controllerType, methodName, args);
    }

    public UriComponentsBuilder withMethodCall(Object invocationInfo) {
        return fromMethodCall(this.baseUrl, invocationInfo);
    }

    public MethodArgumentBuilder withMappingName(String mappingName) {
        return fromMappingName(this.baseUrl, mappingName);
    }

    public UriComponentsBuilder withMethod(Class<?> controllerType, Method method, Object... args) {
        return fromMethod(this.baseUrl, controllerType, method, args);
    }

    private static UriComponentsBuilder fromMethodInternal(@Nullable UriComponentsBuilder builder, Class<?> controllerType, Method method, Object... args) {
        builder = getBaseUrlToUse(builder);
        // Externally configured prefix via PathConfigurer..
        String prefix = getPathPrefix(controllerType);
        builder.path(prefix);
        String typePath = getClassMapping(controllerType);
        String methodPath = getMethodMapping(method);
        String path = pathMatcher.combine(typePath, methodPath);
        builder.path(path);
        return applyContributors(builder, method, args);
    }

    private static UriComponentsBuilder getBaseUrlToUse(@Nullable UriComponentsBuilder baseUrl) {
        return baseUrl == null ? ServletUriComponentsBuilder.fromCurrentServletMapping() : baseUrl.cloneBuilder();
    }

    private static String getPathPrefix(Class<?> controllerType) {
        WebApplicationContext wac = getWebApplicationContext();
        if (wac != null) {
            Map<String, RequestMappingHandlerMapping> map = wac.getBeansOfType(RequestMappingHandlerMapping.class);
            for (RequestMappingHandlerMapping mapping : map.values()) {
                if (mapping.isHandler(controllerType)) {
                    String prefix = mapping.getPathPrefix(controllerType);
                    if (prefix != null) {
                        return prefix;
                    }
                }
            }
        }
        return "";
    }

    private static String getClassMapping(Class<?> controllerType) {
        Assert.notNull(controllerType, "'controllerType' must not be null");
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(controllerType, RequestMapping.class);
        if (mapping == null) {
            return "/";
        }
        String[] paths = mapping.path();
        if (ObjectUtils.isEmpty(paths) || !StringUtils.hasLength(paths[0])) {
            return "/";
        }
        if (paths.length > 1 && logger.isTraceEnabled()) {
            logger.trace("Using first of multiple paths on " + controllerType.getName());
        }
        return paths[0];
    }

    private static String getMethodMapping(Method method) {
        Assert.notNull(method, "'method' must not be null");
        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        if (requestMapping == null) {
            throw new IllegalArgumentException("No @RequestMapping on: " + method.toGenericString());
        }
        String[] paths = requestMapping.path();
        if (ObjectUtils.isEmpty(paths) || !StringUtils.hasLength(paths[0])) {
            return "/";
        }
        if (paths.length > 1 && logger.isTraceEnabled()) {
            logger.trace("Using first of multiple paths on " + method.toGenericString());
        }
        return paths[0];
    }

    private static Method getMethod(Class<?> controllerType, final String methodName, final Object... args) {
        MethodFilter selector = method -> {
            String name = method.getName();
            int argLength = method.getParameterCount();
            return (name.equals(methodName) && argLength == args.length);
        };
        Set<Method> methods = MethodIntrospector.selectMethods(controllerType, selector);
        if (methods.size() == 1) {
            return methods.iterator().next();
        } else if (methods.size() > 1) {
            throw new IllegalArgumentException(String.format("Found two methods named '%s' accepting arguments %s in controller %s: [%s]", methodName, Arrays.asList(args), controllerType.getName(), methods));
        } else {
            throw new IllegalArgumentException("No method named '" + methodName + "' with " + args.length + " arguments found in controller " + controllerType.getName());
        }
    }

    private static UriComponentsBuilder applyContributors(UriComponentsBuilder builder, Method method, Object... args) {
        CompositeUriComponentsContributor contributor = getUriComponentsContributor();
        int paramCount = method.getParameterCount();
        int argCount = args.length;
        if (paramCount != argCount) {
            throw new IllegalArgumentException("Number of method parameters " + paramCount + " does not match number of argument values " + argCount);
        }
        final Map<String, Object> uriVars = new HashMap<>();
        for (int i = 0; i < paramCount; i++) {
            MethodParameter param = new SynthesizingMethodParameter(method, i);
            param.initParameterNameDiscovery(parameterNameDiscoverer);
            contributor.contributeMethodArgument(param, args[i], builder, uriVars);
        }
        // This may not be all the URI variables, supply what we have so far..
        return builder.uriVariables(uriVars);
    }

    private static CompositeUriComponentsContributor getUriComponentsContributor() {
        WebApplicationContext wac = getWebApplicationContext();
        if (wac != null) {
            try {
                return wac.getBean(MVC_URI_COMPONENTS_CONTRIBUTOR_BEAN_NAME, CompositeUriComponentsContributor.class);
            } catch (NoSuchBeanDefinitionException ex) {
                // Ignore
            }
        }
        return defaultUriComponentsContributor;
    }

    @Nullable
    private static WebApplicationContext getWebApplicationContext() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return null;
        }
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        String attributeName = DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE;
        WebApplicationContext wac = (WebApplicationContext) request.getAttribute(attributeName);
        if (wac == null) {
            return null;
        }
        return wac;
    }

    public interface MethodInvocationInfo {

        Class<?> getControllerType();

        Method getControllerMethod();

        Object[] getArgumentValues();

    }

    private static class ControllerMethodInvocationInterceptor implements org.springframework.cglib.proxy.MethodInterceptor, MethodInterceptor, MethodInvocationInfo {

        private final Class<?> controllerType;

        @Nullable
        private Method controllerMethod;

        @Nullable
        private Object[] argumentValues;

        ControllerMethodInvocationInterceptor(Class<?> controllerType) {
            this.controllerType = controllerType;
        }

        @Override
        @Nullable
        public Object intercept(Object obj, Method method, Object[] args, @Nullable MethodProxy proxy) {
            if (method.getName().equals("getControllerType")) {
                return this.controllerType;
            } else if (method.getName().equals("getControllerMethod")) {
                return this.controllerMethod;
            } else if (method.getName().equals("getArgumentValues")) {
                return this.argumentValues;
            } else if (ReflectionUtils.isObjectMethod(method)) {
                return ReflectionUtils.invokeMethod(method, obj, args);
            } else {
                this.controllerMethod = method;
                this.argumentValues = args;
                Class<?> returnType = method.getReturnType();
                try {
                    return (returnType == void.class ? null : returnType.cast(initProxy(returnType, this)));
                } catch (Throwable ex) {
                    throw new IllegalStateException("Failed to create proxy for controller method return type: " + method, ex);
                }
            }
        }

        @Override
        @Nullable
        public Object invoke(org.aopalliance.intercept.MethodInvocation inv) throws Throwable {
            return intercept(inv.getThis(), inv.getMethod(), inv.getArguments(), null);
        }

        @Override
        public Class<?> getControllerType() {
            return this.controllerType;
        }

        @Override
        public Method getControllerMethod() {
            Assert.state(this.controllerMethod != null, "Not initialized yet");
            return this.controllerMethod;
        }

        @Override
        public Object[] getArgumentValues() {
            Assert.state(this.argumentValues != null, "Not initialized yet");
            return this.argumentValues;
        }

        @SuppressWarnings("unchecked")
        private static <T> T initProxy(Class<?> controllerType, @Nullable ControllerMethodInvocationInterceptor interceptor) {
            interceptor = interceptor != null ? interceptor : new ControllerMethodInvocationInterceptor(controllerType);
            if (controllerType == Object.class) {
                return (T) interceptor;
            } else if (controllerType.isInterface()) {
                ProxyFactory factory = new ProxyFactory(EmptyTargetSource.INSTANCE);
                factory.addInterface(controllerType);
                factory.addInterface(MethodInvocationInfo.class);
                factory.addAdvice(interceptor);
                return (T) factory.getProxy();
            } else {
                Enhancer enhancer = new Enhancer();
                enhancer.setSuperclass(controllerType);
                enhancer.setInterfaces(new Class<?>[]{MethodInvocationInfo.class});
                enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
                enhancer.setCallbackType(org.springframework.cglib.proxy.MethodInterceptor.class);
                Class<?> proxyClass = enhancer.createClass();
                Object proxy = null;
                if (objenesis.isWorthTrying()) {
                    try {
                        proxy = objenesis.newInstance(proxyClass, enhancer.getUseCache());
                    } catch (ObjenesisException ex) {
                        logger.debug("Failed to create controller proxy, falling back on default constructor", ex);
                    }
                }
                if (proxy == null) {
                    try {
                        proxy = ReflectionUtils.accessibleConstructor(proxyClass).newInstance();
                    } catch (Throwable ex) {
                        throw new IllegalStateException("Failed to create controller proxy or use default constructor", ex);
                    }
                }
                ((Factory) proxy).setCallbacks(new Callback[]{interceptor});
                return (T) proxy;
            }
        }

    }

    public static class MethodArgumentBuilder {

        private final Class<?> controllerType;

        private final Method method;

        private final Object[] argumentValues;

        private final UriComponentsBuilder baseUrl;

        public MethodArgumentBuilder(Class<?> controllerType, Method method) {
            this(null, controllerType, method);
        }

        public MethodArgumentBuilder(@Nullable UriComponentsBuilder baseUrl, Class<?> controllerType, Method method) {
            Assert.notNull(controllerType, "'controllerType' is required");
            Assert.notNull(method, "'method' is required");
            this.baseUrl = baseUrl != null ? baseUrl : UriComponentsBuilder.fromPath(getPath());
            this.controllerType = controllerType;
            this.method = method;
            this.argumentValues = new Object[method.getParameterCount()];
        }

        private static String getPath() {
            UriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentServletMapping();
            String path = builder.build().getPath();
            return path != null ? path : "";
        }

        public MethodArgumentBuilder arg(int index, Object value) {
            this.argumentValues[index] = value;
            return this;
        }

        public MethodArgumentBuilder encode() {
            this.baseUrl.encode();
            return this;
        }

        public String build() {
            return fromMethodInternal(this.baseUrl, this.controllerType, this.method, this.argumentValues).build().encode().toUriString();
        }

        public String buildAndExpand(Object... uriVars) {
            return fromMethodInternal(this.baseUrl, this.controllerType, this.method, this.argumentValues).buildAndExpand(uriVars).encode().toString();
        }

    }

}
