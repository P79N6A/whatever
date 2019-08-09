package org.springframework.boot.env;

import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PropertiesPropertySourceLoader implements PropertySourceLoader {

    private static final String XML_FILE_EXTENSION = ".xml";

    @Override
    public String[] getFileExtensions() {
        return new String[]{"properties", "xml"};
    }

    @Override
    public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
        Map<String, ?> properties = loadProperties(resource);
        if (properties.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new OriginTrackedMapPropertySource(name, properties));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, ?> loadProperties(Resource resource) throws IOException {
        String filename = resource.getFilename();
        // xml
        if (filename != null && filename.endsWith(XML_FILE_EXTENSION)) {
            return (Map) PropertiesLoaderUtils.loadProperties(resource);
        }
        // properties
        return new OriginTrackedPropertiesLoader(resource).load();
    }

}
