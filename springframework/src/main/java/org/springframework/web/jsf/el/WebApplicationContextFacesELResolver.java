package org.springframework.web.jsf.el;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.jsf.FacesContextUtils;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.faces.context.FacesContext;
import java.beans.FeatureDescriptor;
import java.util.Iterator;

public class WebApplicationContextFacesELResolver extends ELResolver {

    public static final String WEB_APPLICATION_CONTEXT_VARIABLE_NAME = "webApplicationContext";

    protected final Log logger = LogFactory.getLog(getClass());

    @Override
    @Nullable
    public Object getValue(ELContext elContext, @Nullable Object base, Object property) throws ELException {
        if (base != null) {
            if (base instanceof WebApplicationContext) {
                WebApplicationContext wac = (WebApplicationContext) base;
                String beanName = property.toString();
                if (logger.isTraceEnabled()) {
                    logger.trace("Attempting to resolve property '" + beanName + "' in root WebApplicationContext");
                }
                if (wac.containsBean(beanName)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Successfully resolved property '" + beanName + "' in root WebApplicationContext");
                    }
                    elContext.setPropertyResolved(true);
                    try {
                        return wac.getBean(beanName);
                    } catch (BeansException ex) {
                        throw new ELException(ex);
                    }
                } else {
                    // Mimic standard JSF/JSP behavior when base is a Map by returning null.
                    return null;
                }
            }
        } else {
            if (WEB_APPLICATION_CONTEXT_VARIABLE_NAME.equals(property)) {
                elContext.setPropertyResolved(true);
                return getWebApplicationContext(elContext);
            }
        }
        return null;
    }

    @Override
    @Nullable
    public Class<?> getType(ELContext elContext, @Nullable Object base, Object property) throws ELException {
        if (base != null) {
            if (base instanceof WebApplicationContext) {
                WebApplicationContext wac = (WebApplicationContext) base;
                String beanName = property.toString();
                if (logger.isDebugEnabled()) {
                    logger.debug("Attempting to resolve property '" + beanName + "' in root WebApplicationContext");
                }
                if (wac.containsBean(beanName)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Successfully resolved property '" + beanName + "' in root WebApplicationContext");
                    }
                    elContext.setPropertyResolved(true);
                    try {
                        return wac.getType(beanName);
                    } catch (BeansException ex) {
                        throw new ELException(ex);
                    }
                } else {
                    // Mimic standard JSF/JSP behavior when base is a Map by returning null.
                    return null;
                }
            }
        } else {
            if (WEB_APPLICATION_CONTEXT_VARIABLE_NAME.equals(property)) {
                elContext.setPropertyResolved(true);
                return WebApplicationContext.class;
            }
        }
        return null;
    }

    @Override
    public void setValue(ELContext elContext, Object base, Object property, Object value) throws ELException {
    }

    @Override
    public boolean isReadOnly(ELContext elContext, Object base, Object property) throws ELException {
        if (base instanceof WebApplicationContext) {
            elContext.setPropertyResolved(true);
            return true;
        }
        return false;
    }

    @Override
    @Nullable
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext elContext, Object base) {
        return null;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext elContext, Object base) {
        return Object.class;
    }

    @Nullable
    protected WebApplicationContext getWebApplicationContext(ELContext elContext) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        return FacesContextUtils.getRequiredWebApplicationContext(facesContext);
    }

}
