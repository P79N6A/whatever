package org.springframework.web.servlet.mvc.support;

import org.springframework.lang.Nullable;
import org.springframework.ui.ModelMap;
import org.springframework.validation.DataBinder;

import java.util.Collection;
import java.util.Map;

@SuppressWarnings("serial")
public class RedirectAttributesModelMap extends ModelMap implements RedirectAttributes {

    @Nullable
    private final DataBinder dataBinder;

    private final ModelMap flashAttributes = new ModelMap();

    public RedirectAttributesModelMap() {
        this(null);
    }

    public RedirectAttributesModelMap(@Nullable DataBinder dataBinder) {
        this.dataBinder = dataBinder;
    }

    @Override
    public Map<String, ?> getFlashAttributes() {
        return this.flashAttributes;
    }

    @Override
    public RedirectAttributesModelMap addAttribute(String attributeName, @Nullable Object attributeValue) {
        super.addAttribute(attributeName, formatValue(attributeValue));
        return this;
    }

    @Nullable
    private String formatValue(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        return (this.dataBinder != null ? this.dataBinder.convertIfNecessary(value, String.class) : value.toString());
    }

    @Override
    public RedirectAttributesModelMap addAttribute(Object attributeValue) {
        super.addAttribute(attributeValue);
        return this;
    }

    @Override
    public RedirectAttributesModelMap addAllAttributes(@Nullable Collection<?> attributeValues) {
        super.addAllAttributes(attributeValues);
        return this;
    }

    @Override
    public RedirectAttributesModelMap addAllAttributes(@Nullable Map<String, ?> attributes) {
        if (attributes != null) {
            attributes.forEach(this::addAttribute);
        }
        return this;
    }

    @Override
    public RedirectAttributesModelMap mergeAttributes(@Nullable Map<String, ?> attributes) {
        if (attributes != null) {
            attributes.forEach((key, attribute) -> {
                if (!containsKey(key)) {
                    addAttribute(key, attribute);
                }
            });
        }
        return this;
    }

    @Override
    public Map<String, Object> asMap() {
        return this;
    }

    @Override
    public Object put(String key, @Nullable Object value) {
        return super.put(key, formatValue(value));
    }

    @Override
    public void putAll(@Nullable Map<? extends String, ? extends Object> map) {
        if (map != null) {
            map.forEach((key, value) -> put(key, formatValue(value)));
        }
    }

    @Override
    public RedirectAttributes addFlashAttribute(String attributeName, @Nullable Object attributeValue) {
        this.flashAttributes.addAttribute(attributeName, attributeValue);
        return this;
    }

    @Override
    public RedirectAttributes addFlashAttribute(Object attributeValue) {
        this.flashAttributes.addAttribute(attributeValue);
        return this;
    }

}
