package org.springframework.web.jsf;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.web.context.WebApplicationContext;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import java.util.Collection;

@SuppressWarnings("serial")
public class DelegatingPhaseListenerMulticaster implements PhaseListener {

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.ANY_PHASE;
    }

    @Override
    public void beforePhase(PhaseEvent event) {
        for (PhaseListener listener : getDelegates(event.getFacesContext())) {
            listener.beforePhase(event);
        }
    }

    @Override
    public void afterPhase(PhaseEvent event) {
        for (PhaseListener listener : getDelegates(event.getFacesContext())) {
            listener.afterPhase(event);
        }
    }

    protected Collection<PhaseListener> getDelegates(FacesContext facesContext) {
        ListableBeanFactory bf = getBeanFactory(facesContext);
        return BeanFactoryUtils.beansOfTypeIncludingAncestors(bf, PhaseListener.class, true, false).values();
    }

    protected ListableBeanFactory getBeanFactory(FacesContext facesContext) {
        return getWebApplicationContext(facesContext);
    }

    protected WebApplicationContext getWebApplicationContext(FacesContext facesContext) {
        return FacesContextUtils.getRequiredWebApplicationContext(facesContext);
    }

}
