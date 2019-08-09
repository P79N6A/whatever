package org.springframework.web.jsf.el;

import org.springframework.lang.Nullable;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.jsf.FacesContextUtils;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotWritableException;
import javax.faces.context.FacesContext;
import java.beans.FeatureDescriptor;
import java.util.Iterator;

public class SpringBeanFacesELResolver extends ELResolver {

    @Override
    @Nullable
    public Object getValue(ELContext elContext, @Nullable Object base, Object property) throws ELException {
        if (base == null) {
            String beanName = property.toString();
            WebApplicationContext wac = getWebApplicationContext(elContext);
            if (wac.containsBean(beanName)) {
                elContext.setPropertyResolved(true);
                return wac.getBean(beanName);
            }
        }
        return null;
    }

    @Override
    @Nullable
    public Class<?> getType(ELContext elContext, @Nullable Object base, Object property) throws ELException {
        if (base == null) {
            String beanName = property.toString();
            WebApplicationContext wac = getWebApplicationContext(elContext);
            if (wac.containsBean(beanName)) {
                elContext.setPropertyResolved(true);
                return wac.getType(beanName);
            }
        }
        return null;
    }

    @Override
    public void setValue(ELContext elContext, @Nullable Object base, Object property, Object value) throws ELException {
        if (base == null) {
            String beanName = property.toString();
            WebApplicationContext wac = getWebApplicationContext(elContext);
            if (wac.containsBean(beanName)) {
                if (value == wac.getBean(beanName)) {
                    // Setting the bean reference to the same value is alright - can simply be ignored...
                    elContext.setPropertyResolved(true);
                } else {
                    throw new PropertyNotWritableException("Variable '" + beanName + "' refers to a Spring bean which by definition is not writable");
                }
            }
        }
    }

    @Override
    public boolean isReadOnly(ELContext elContext, @Nullable Object base, Object property) throws ELException {
        if (base == null) {
            String beanName = property.toString();
            WebApplicationContext wac = getWebApplicationContext(elContext);
            if (wac.containsBean(beanName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Nullable
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext elContext, @Nullable Object base) {
        return null;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext elContext, @Nullable Object base) {
        return Object.class;
    }

    protected WebApplicationContext getWebApplicationContext(ELContext elContext) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        return FacesContextUtils.getRequiredWebApplicationContext(facesContext);
    }

}
