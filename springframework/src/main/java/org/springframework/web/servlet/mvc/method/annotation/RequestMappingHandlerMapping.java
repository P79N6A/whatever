package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.MatchableHandlerMapping;
import org.springframework.web.servlet.handler.RequestMatchResult;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Predicate;

public class RequestMappingHandlerMapping extends RequestMappingInfoHandlerMapping implements MatchableHandlerMapping, EmbeddedValueResolverAware {

    private boolean useSuffixPatternMatch = true;

    private boolean useRegisteredSuffixPatternMatch = false;

    private boolean useTrailingSlashMatch = true;

    private Map<String, Predicate<Class<?>>> pathPrefixes = new LinkedHashMap<>();

    private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

    @Nullable
    private StringValueResolver embeddedValueResolver;

    private RequestMappingInfo.BuilderConfiguration config = new RequestMappingInfo.BuilderConfiguration();

    public void setUseSuffixPatternMatch(boolean useSuffixPatternMatch) {
        this.useSuffixPatternMatch = useSuffixPatternMatch;
    }

    public void setUseRegisteredSuffixPatternMatch(boolean useRegisteredSuffixPatternMatch) {
        this.useRegisteredSuffixPatternMatch = useRegisteredSuffixPatternMatch;
        this.useSuffixPatternMatch = (useRegisteredSuffixPatternMatch || this.useSuffixPatternMatch);
    }

    public void setUseTrailingSlashMatch(boolean useTrailingSlashMatch) {
        this.useTrailingSlashMatch = useTrailingSlashMatch;
    }

    public void setPathPrefixes(Map<String, Predicate<Class<?>>> prefixes) {
        this.pathPrefixes = Collections.unmodifiableMap(new LinkedHashMap<>(prefixes));
    }

    public Map<String, Predicate<Class<?>>> getPathPrefixes() {
        return this.pathPrefixes;
    }

    public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
        Assert.notNull(contentNegotiationManager, "ContentNegotiationManager must not be null");
        this.contentNegotiationManager = contentNegotiationManager;
    }

    public ContentNegotiationManager getContentNegotiationManager() {
        return this.contentNegotiationManager;
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.embeddedValueResolver = resolver;
    }

    @Override
    public void afterPropertiesSet() {
        this.config = new RequestMappingInfo.BuilderConfiguration();
        this.config.setUrlPathHelper(getUrlPathHelper());
        this.config.setPathMatcher(getPathMatcher());
        this.config.setSuffixPatternMatch(this.useSuffixPatternMatch);
        this.config.setTrailingSlashMatch(this.useTrailingSlashMatch);
        this.config.setRegisteredSuffixPatternMatch(this.useRegisteredSuffixPatternMatch);
        this.config.setContentNegotiationManager(getContentNegotiationManager());
        super.afterPropertiesSet();
    }

    public boolean useSuffixPatternMatch() {
        return this.useSuffixPatternMatch;
    }

    public boolean useRegisteredSuffixPatternMatch() {
        return this.useRegisteredSuffixPatternMatch;
    }

    public boolean useTrailingSlashMatch() {
        return this.useTrailingSlashMatch;
    }

    @Nullable
    public List<String> getFileExtensions() {
        return this.config.getFileExtensions();
    }

    @Override
    protected boolean isHandler(Class<?> beanType) {
        return (AnnotatedElementUtils.hasAnnotation(beanType, Controller.class) || AnnotatedElementUtils.hasAnnotation(beanType, RequestMapping.class));
    }

    @Override
    @Nullable
    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
        // 从方法获取@RequestMapping的信息，封装成RequestMappingInfo对象
        RequestMappingInfo info = createRequestMappingInfo(method);
        if (info != null) {
            // 从类获取@RequestMapping的信息，封装成RequestMappingInfo对象
            RequestMappingInfo typeInfo = createRequestMappingInfo(handlerType);
            if (typeInfo != null) {
                // 合并
                info = typeInfo.combine(info);
            }
            // 从类获取路径的前缀
            String prefix = getPathPrefix(handlerType);
            if (prefix != null) {
                // 合并
                info = RequestMappingInfo.paths(prefix).build().combine(info);
            }
        }
        return info;
    }

    @Nullable
    String getPathPrefix(Class<?> handlerType) {
        for (Map.Entry<String, Predicate<Class<?>>> entry : this.pathPrefixes.entrySet()) {
            if (entry.getValue().test(handlerType)) {
                String prefix = entry.getKey();
                if (this.embeddedValueResolver != null) {
                    prefix = this.embeddedValueResolver.resolveStringValue(prefix);
                }
                return prefix;
            }
        }
        return null;
    }

    @Nullable
    private RequestMappingInfo createRequestMappingInfo(AnnotatedElement element) {
        // @RequestMapping
        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class);
        // 类对象返回null，实例对象返回null
        RequestCondition<?> condition = (element instanceof Class ? getCustomTypeCondition((Class<?>) element) : getCustomMethodCondition((Method) element));
        return (requestMapping != null ? createRequestMappingInfo(requestMapping, condition) : null);
    }

    @Nullable
    protected RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
        return null;
    }

    @Nullable
    protected RequestCondition<?> getCustomMethodCondition(Method method) {
        return null;
    }


    /**
     * 把@RequestMapping实例的信息封装成RequestMappingInfo对象返回
     */
    protected RequestMappingInfo createRequestMappingInfo(RequestMapping requestMapping, @Nullable RequestCondition<?> customCondition) {
        RequestMappingInfo.Builder builder = RequestMappingInfo.paths(
                resolveEmbeddedValuesInPatterns(requestMapping.path()) // @RequestMapping注解的path值
        )
                .methods(requestMapping.method()) // @RequestMapping注解的method值
                .params(requestMapping.params()) // @RequestMapping注解的params值
                .headers(requestMapping.headers()) // @RequestMapping注解的headers值
                .consumes(requestMapping.consumes()) // @RequestMapping注解的consumes值
                .produces(requestMapping.produces()) // @RequestMapping注解的produces值
                .mappingName(requestMapping.name()); // @RequestMapping注解的name值
        if (customCondition != null) {
            builder.customCondition(customCondition);
        }
        return builder.options(this.config).build();
    }

    protected String[] resolveEmbeddedValuesInPatterns(String[] patterns) {
        if (this.embeddedValueResolver == null) {
            return patterns;
        } else {
            String[] resolvedPatterns = new String[patterns.length];
            for (int i = 0; i < patterns.length; i++) {
                resolvedPatterns[i] = this.embeddedValueResolver.resolveStringValue(patterns[i]);
            }
            return resolvedPatterns;
        }
    }

    @Override
    public void registerMapping(RequestMappingInfo mapping, Object handler, Method method) {
        super.registerMapping(mapping, handler, method);
        updateConsumesCondition(mapping, method);
    }

    @Override
    protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
        super.registerHandlerMethod(handler, method, mapping);
        updateConsumesCondition(mapping, method);
    }

    private void updateConsumesCondition(RequestMappingInfo info, Method method) {
        ConsumesRequestCondition condition = info.getConsumesCondition();
        if (!condition.isEmpty()) {
            for (Parameter parameter : method.getParameters()) {
                MergedAnnotation<RequestBody> annot = MergedAnnotations.from(parameter).get(RequestBody.class);
                if (annot.isPresent()) {
                    condition.setBodyRequired(annot.getBoolean("required"));
                    break;
                }
            }
        }
    }

    @Override
    public RequestMatchResult match(HttpServletRequest request, String pattern) {
        RequestMappingInfo info = RequestMappingInfo.paths(pattern).options(this.config).build();
        RequestMappingInfo matchingInfo = info.getMatchingCondition(request);
        if (matchingInfo == null) {
            return null;
        }
        Set<String> patterns = matchingInfo.getPatternsCondition().getPatterns();
        String lookupPath = getUrlPathHelper().getLookupPathForRequest(request, LOOKUP_PATH);
        return new RequestMatchResult(patterns.iterator().next(), lookupPath, getPathMatcher());
    }

    @Override
    protected CorsConfiguration initCorsConfiguration(Object handler, Method method, RequestMappingInfo mappingInfo) {
        HandlerMethod handlerMethod = createHandlerMethod(handler, method);
        Class<?> beanType = handlerMethod.getBeanType();
        CrossOrigin typeAnnotation = AnnotatedElementUtils.findMergedAnnotation(beanType, CrossOrigin.class);
        CrossOrigin methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, CrossOrigin.class);
        if (typeAnnotation == null && methodAnnotation == null) {
            return null;
        }
        CorsConfiguration config = new CorsConfiguration();
        updateCorsConfig(config, typeAnnotation);
        updateCorsConfig(config, methodAnnotation);
        if (CollectionUtils.isEmpty(config.getAllowedMethods())) {
            for (RequestMethod allowedMethod : mappingInfo.getMethodsCondition().getMethods()) {
                config.addAllowedMethod(allowedMethod.name());
            }
        }
        return config.applyPermitDefaultValues();
    }

    private void updateCorsConfig(CorsConfiguration config, @Nullable CrossOrigin annotation) {
        if (annotation == null) {
            return;
        }
        for (String origin : annotation.origins()) {
            config.addAllowedOrigin(resolveCorsAnnotationValue(origin));
        }
        for (RequestMethod method : annotation.methods()) {
            config.addAllowedMethod(method.name());
        }
        for (String header : annotation.allowedHeaders()) {
            config.addAllowedHeader(resolveCorsAnnotationValue(header));
        }
        for (String header : annotation.exposedHeaders()) {
            config.addExposedHeader(resolveCorsAnnotationValue(header));
        }
        String allowCredentials = resolveCorsAnnotationValue(annotation.allowCredentials());
        if ("true".equalsIgnoreCase(allowCredentials)) {
            config.setAllowCredentials(true);
        } else if ("false".equalsIgnoreCase(allowCredentials)) {
            config.setAllowCredentials(false);
        } else if (!allowCredentials.isEmpty()) {
            throw new IllegalStateException("@CrossOrigin's allowCredentials value must be \"true\", \"false\", " + "or an empty string (\"\"): current value is [" + allowCredentials + "]");
        }
        if (annotation.maxAge() >= 0 && config.getMaxAge() == null) {
            config.setMaxAge(annotation.maxAge());
        }
    }

    private String resolveCorsAnnotationValue(String value) {
        if (this.embeddedValueResolver != null) {
            String resolved = this.embeddedValueResolver.resolveStringValue(value);
            return (resolved != null ? resolved : "");
        } else {
            return value;
        }
    }

}
