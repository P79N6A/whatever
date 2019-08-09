package org.springframework.web.servlet.view;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationManagerFactoryBean;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class ContentNegotiatingViewResolver extends WebApplicationObjectSupport implements ViewResolver, Ordered, InitializingBean {

    @Nullable
    private ContentNegotiationManager contentNegotiationManager;

    private final ContentNegotiationManagerFactoryBean cnmFactoryBean = new ContentNegotiationManagerFactoryBean();

    private boolean useNotAcceptableStatusCode = false;

    @Nullable
    private List<View> defaultViews;

    @Nullable
    private List<ViewResolver> viewResolvers;

    private int order = Ordered.HIGHEST_PRECEDENCE;

    public void setContentNegotiationManager(@Nullable ContentNegotiationManager contentNegotiationManager) {
        this.contentNegotiationManager = contentNegotiationManager;
    }

    @Nullable
    public ContentNegotiationManager getContentNegotiationManager() {
        return this.contentNegotiationManager;
    }

    public void setUseNotAcceptableStatusCode(boolean useNotAcceptableStatusCode) {
        this.useNotAcceptableStatusCode = useNotAcceptableStatusCode;
    }

    public boolean isUseNotAcceptableStatusCode() {
        return this.useNotAcceptableStatusCode;
    }

    public void setDefaultViews(List<View> defaultViews) {
        this.defaultViews = defaultViews;
    }

    public List<View> getDefaultViews() {
        return (this.defaultViews != null ? Collections.unmodifiableList(this.defaultViews) : Collections.emptyList());
    }

    public void setViewResolvers(List<ViewResolver> viewResolvers) {
        this.viewResolvers = viewResolvers;
    }

    public List<ViewResolver> getViewResolvers() {
        return (this.viewResolvers != null ? Collections.unmodifiableList(this.viewResolvers) : Collections.emptyList());
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    protected void initServletContext(ServletContext servletContext) {
        Collection<ViewResolver> matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(obtainApplicationContext(), ViewResolver.class).values();
        if (this.viewResolvers == null) {
            this.viewResolvers = new ArrayList<>(matchingBeans.size());
            for (ViewResolver viewResolver : matchingBeans) {
                if (this != viewResolver) {
                    this.viewResolvers.add(viewResolver);
                }
            }
        } else {
            for (int i = 0; i < this.viewResolvers.size(); i++) {
                ViewResolver vr = this.viewResolvers.get(i);
                if (matchingBeans.contains(vr)) {
                    continue;
                }
                String name = vr.getClass().getName() + i;
                obtainApplicationContext().getAutowireCapableBeanFactory().initializeBean(vr, name);
            }

        }
        AnnotationAwareOrderComparator.sort(this.viewResolvers);
        this.cnmFactoryBean.setServletContext(servletContext);
    }

    @Override
    public void afterPropertiesSet() {
        if (this.contentNegotiationManager == null) {
            this.contentNegotiationManager = this.cnmFactoryBean.build();
        }
        if (this.viewResolvers == null || this.viewResolvers.isEmpty()) {
            logger.warn("No ViewResolvers configured");
        }
    }

    @Override
    @Nullable
    public View resolveViewName(String viewName, Locale locale) throws Exception {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        Assert.state(attrs instanceof ServletRequestAttributes, "No current ServletRequestAttributes");
        List<MediaType> requestedMediaTypes = getMediaTypes(((ServletRequestAttributes) attrs).getRequest());
        if (requestedMediaTypes != null) {
            List<View> candidateViews = getCandidateViews(viewName, locale, requestedMediaTypes);
            View bestView = getBestView(candidateViews, requestedMediaTypes, attrs);
            if (bestView != null) {
                return bestView;
            }
        }
        String mediaTypeInfo = logger.isDebugEnabled() && requestedMediaTypes != null ? " given " + requestedMediaTypes.toString() : "";
        if (this.useNotAcceptableStatusCode) {
            if (logger.isDebugEnabled()) {
                logger.debug("Using 406 NOT_ACCEPTABLE" + mediaTypeInfo);
            }
            return NOT_ACCEPTABLE_VIEW;
        } else {
            logger.debug("View remains unresolved" + mediaTypeInfo);
            return null;
        }
    }

    @Nullable
    protected List<MediaType> getMediaTypes(HttpServletRequest request) {
        Assert.state(this.contentNegotiationManager != null, "No ContentNegotiationManager set");
        try {
            ServletWebRequest webRequest = new ServletWebRequest(request);
            List<MediaType> acceptableMediaTypes = this.contentNegotiationManager.resolveMediaTypes(webRequest);
            List<MediaType> producibleMediaTypes = getProducibleMediaTypes(request);
            Set<MediaType> compatibleMediaTypes = new LinkedHashSet<>();
            for (MediaType acceptable : acceptableMediaTypes) {
                for (MediaType producible : producibleMediaTypes) {
                    if (acceptable.isCompatibleWith(producible)) {
                        compatibleMediaTypes.add(getMostSpecificMediaType(acceptable, producible));
                    }
                }
            }
            List<MediaType> selectedMediaTypes = new ArrayList<>(compatibleMediaTypes);
            MediaType.sortBySpecificityAndQuality(selectedMediaTypes);
            return selectedMediaTypes;
        } catch (HttpMediaTypeNotAcceptableException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug(ex.getMessage());
            }
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<MediaType> getProducibleMediaTypes(HttpServletRequest request) {
        Set<MediaType> mediaTypes = (Set<MediaType>) request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
        if (!CollectionUtils.isEmpty(mediaTypes)) {
            return new ArrayList<>(mediaTypes);
        } else {
            return Collections.singletonList(MediaType.ALL);
        }
    }

    private MediaType getMostSpecificMediaType(MediaType acceptType, MediaType produceType) {
        produceType = produceType.copyQualityValue(acceptType);
        return (MediaType.SPECIFICITY_COMPARATOR.compare(acceptType, produceType) < 0 ? acceptType : produceType);
    }

    private List<View> getCandidateViews(String viewName, Locale locale, List<MediaType> requestedMediaTypes) throws Exception {
        List<View> candidateViews = new ArrayList<>();
        if (this.viewResolvers != null) {
            Assert.state(this.contentNegotiationManager != null, "No ContentNegotiationManager set");
            for (ViewResolver viewResolver : this.viewResolvers) {
                View view = viewResolver.resolveViewName(viewName, locale);
                if (view != null) {
                    candidateViews.add(view);
                }
                for (MediaType requestedMediaType : requestedMediaTypes) {
                    List<String> extensions = this.contentNegotiationManager.resolveFileExtensions(requestedMediaType);
                    for (String extension : extensions) {
                        String viewNameWithExtension = viewName + '.' + extension;
                        view = viewResolver.resolveViewName(viewNameWithExtension, locale);
                        if (view != null) {
                            candidateViews.add(view);
                        }
                    }
                }
            }
        }
        if (!CollectionUtils.isEmpty(this.defaultViews)) {
            candidateViews.addAll(this.defaultViews);
        }
        return candidateViews;
    }

    @Nullable
    private View getBestView(List<View> candidateViews, List<MediaType> requestedMediaTypes, RequestAttributes attrs) {
        for (View candidateView : candidateViews) {
            if (candidateView instanceof SmartView) {
                SmartView smartView = (SmartView) candidateView;
                if (smartView.isRedirectView()) {
                    return candidateView;
                }
            }
        }
        for (MediaType mediaType : requestedMediaTypes) {
            for (View candidateView : candidateViews) {
                if (StringUtils.hasText(candidateView.getContentType())) {
                    MediaType candidateContentType = MediaType.parseMediaType(candidateView.getContentType());
                    if (mediaType.isCompatibleWith(candidateContentType)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Selected '" + mediaType + "' given " + requestedMediaTypes);
                        }
                        attrs.setAttribute(View.SELECTED_CONTENT_TYPE, mediaType, RequestAttributes.SCOPE_REQUEST);
                        return candidateView;
                    }
                }
            }
        }
        return null;
    }

    private static final View NOT_ACCEPTABLE_VIEW = new View() {

        @Override
        @Nullable
        public String getContentType() {
            return null;
        }

        @Override
        public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) {
            response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
        }
    };

}
