package org.springframework.web.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

@Deprecated
public class DefaultUriTemplateHandler extends AbstractUriTemplateHandler {

    private boolean parsePath;

    private boolean strictEncoding;

    public void setParsePath(boolean parsePath) {
        this.parsePath = parsePath;
    }

    public boolean shouldParsePath() {
        return this.parsePath;
    }

    public void setStrictEncoding(boolean strictEncoding) {
        this.strictEncoding = strictEncoding;
    }

    public boolean isStrictEncoding() {
        return this.strictEncoding;
    }

    @Override
    protected URI expandInternal(String uriTemplate, Map<String, ?> uriVariables) {
        UriComponentsBuilder uriComponentsBuilder = initUriComponentsBuilder(uriTemplate);
        UriComponents uriComponents = expandAndEncode(uriComponentsBuilder, uriVariables);
        return createUri(uriComponents);
    }

    @Override
    protected URI expandInternal(String uriTemplate, Object... uriVariables) {
        UriComponentsBuilder uriComponentsBuilder = initUriComponentsBuilder(uriTemplate);
        UriComponents uriComponents = expandAndEncode(uriComponentsBuilder, uriVariables);
        return createUri(uriComponents);
    }

    protected UriComponentsBuilder initUriComponentsBuilder(String uriTemplate) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uriTemplate);
        if (shouldParsePath() && !isStrictEncoding()) {
            List<String> pathSegments = builder.build().getPathSegments();
            builder.replacePath(null);
            for (String pathSegment : pathSegments) {
                builder.pathSegment(pathSegment);
            }
        }
        return builder;
    }

    protected UriComponents expandAndEncode(UriComponentsBuilder builder, Map<String, ?> uriVariables) {
        if (!isStrictEncoding()) {
            return builder.buildAndExpand(uriVariables).encode();
        } else {
            Map<String, ?> encodedUriVars = UriUtils.encodeUriVariables(uriVariables);
            return builder.buildAndExpand(encodedUriVars);
        }
    }

    protected UriComponents expandAndEncode(UriComponentsBuilder builder, Object[] uriVariables) {
        if (!isStrictEncoding()) {
            return builder.buildAndExpand(uriVariables).encode();
        } else {
            Object[] encodedUriVars = UriUtils.encodeUriVariables(uriVariables);
            return builder.buildAndExpand(encodedUriVars);
        }
    }

    private URI createUri(UriComponents uriComponents) {
        try {
            // Avoid further encoding (in the case of strictEncoding=true)
            return new URI(uriComponents.toUriString());
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("Could not create URI object: " + ex.getMessage(), ex);
        }
    }

}
