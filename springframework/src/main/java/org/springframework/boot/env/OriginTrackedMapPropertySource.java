package org.springframework.boot.env;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

public final class OriginTrackedMapPropertySource extends MapPropertySource implements OriginLookup<String> {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public OriginTrackedMapPropertySource(String name, Map source) {
        super(name, source);
    }

    @Override
    public Object getProperty(String name) {
        Object value = super.getProperty(name);
        if (value instanceof OriginTrackedValue) {
            return ((OriginTrackedValue) value).getValue();
        }
        return value;
    }

    @Override
    public Origin getOrigin(String name) {
        Object value = super.getProperty(name);
        if (value instanceof OriginTrackedValue) {
            return ((OriginTrackedValue) value).getOrigin();
        }
        return null;
    }

}
