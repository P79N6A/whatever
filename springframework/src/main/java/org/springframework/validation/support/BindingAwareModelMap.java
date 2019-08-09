package org.springframework.validation.support;

import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BindingResult;

import java.util.Map;

@SuppressWarnings("serial")
public class BindingAwareModelMap extends ExtendedModelMap {

    @Override
    public Object put(String key, Object value) {
        removeBindingResultIfNecessary(key, value);
        return super.put(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ?> map) {
        map.forEach(this::removeBindingResultIfNecessary);
        super.putAll(map);
    }

    private void removeBindingResultIfNecessary(Object key, Object value) {
        if (key instanceof String) {
            String attributeName = (String) key;
            if (!attributeName.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
                String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + attributeName;
                BindingResult bindingResult = (BindingResult) get(bindingResultKey);
                if (bindingResult != null && bindingResult.getTarget() != value) {
                    remove(bindingResultKey);
                }
            }
        }
    }

}
