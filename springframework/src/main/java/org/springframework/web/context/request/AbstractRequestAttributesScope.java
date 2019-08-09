package org.springframework.web.context.request;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.lang.Nullable;

public abstract class AbstractRequestAttributesScope implements Scope {

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
        Object scopedObject = attributes.getAttribute(name, getScope());
        if (scopedObject == null) {
            scopedObject = objectFactory.getObject();
            attributes.setAttribute(name, scopedObject, getScope());
            // Retrieve object again, registering it for implicit session attribute updates.
            // As a bonus, we also allow for potential decoration at the getAttribute level.
            Object retrievedObject = attributes.getAttribute(name, getScope());
            if (retrievedObject != null) {
                // Only proceed with retrieved object if still present (the expected case).
                // If it disappeared concurrently, we return our locally created instance.
                scopedObject = retrievedObject;
            }
        }
        return scopedObject;
    }

    @Override
    @Nullable
    public Object remove(String name) {
        RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
        Object scopedObject = attributes.getAttribute(name, getScope());
        if (scopedObject != null) {
            attributes.removeAttribute(name, getScope());
            return scopedObject;
        } else {
            return null;
        }
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
        RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
        attributes.registerDestructionCallback(name, callback, getScope());
    }

    @Override
    @Nullable
    public Object resolveContextualObject(String key) {
        RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
        return attributes.resolveReference(key);
    }

    protected abstract int getScope();

}
