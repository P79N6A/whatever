package org.springframework.boot.env;

import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;

public interface PropertySourceLoader {

    String[] getFileExtensions();

    List<PropertySource<?>> load(String name, Resource resource) throws IOException;

}
