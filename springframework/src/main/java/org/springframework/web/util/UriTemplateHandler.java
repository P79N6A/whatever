package org.springframework.web.util;

import java.net.URI;
import java.util.Map;

public interface UriTemplateHandler {

    URI expand(String uriTemplate, Map<String, ?> uriVariables);

    URI expand(String uriTemplate, Object... uriVariables);

}
