package org.springframework.core.env;

import java.util.Map;
import java.util.Properties;

public class PropertiesPropertySource extends MapPropertySource {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public PropertiesPropertySource(String name, Properties source) {
        super(name, (Map) source);
    }

    protected PropertiesPropertySource(String name, Map<String, Object> source) {
        super(name, source);
    }

}
