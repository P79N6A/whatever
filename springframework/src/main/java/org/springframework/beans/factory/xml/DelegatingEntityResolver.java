package org.springframework.beans.factory.xml;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;

public class DelegatingEntityResolver implements EntityResolver {

    public static final String DTD_SUFFIX = ".dtd";

    public static final String XSD_SUFFIX = ".xsd";

    private final EntityResolver dtdResolver;

    private final EntityResolver schemaResolver;

    public DelegatingEntityResolver(@Nullable ClassLoader classLoader) {
        this.dtdResolver = new BeansDtdResolver();
        this.schemaResolver = new PluggableSchemaResolver(classLoader);
    }

    public DelegatingEntityResolver(EntityResolver dtdResolver, EntityResolver schemaResolver) {
        Assert.notNull(dtdResolver, "'dtdResolver' is required");
        Assert.notNull(schemaResolver, "'schemaResolver' is required");
        this.dtdResolver = dtdResolver;
        this.schemaResolver = schemaResolver;
    }

    @Override
    @Nullable
    public InputSource resolveEntity(@Nullable String publicId, @Nullable String systemId) throws SAXException, IOException {
        if (systemId != null) {
            if (systemId.endsWith(DTD_SUFFIX)) {
                return this.dtdResolver.resolveEntity(publicId, systemId);
            } else if (systemId.endsWith(XSD_SUFFIX)) {
                return this.schemaResolver.resolveEntity(publicId, systemId);
            }
        }
        // Fall back to the parser's default behavior.
        return null;
    }

    @Override
    public String toString() {
        return "EntityResolver delegating " + XSD_SUFFIX + " to " + this.schemaResolver + " and " + DTD_SUFFIX + " to " + this.dtdResolver;
    }

}
