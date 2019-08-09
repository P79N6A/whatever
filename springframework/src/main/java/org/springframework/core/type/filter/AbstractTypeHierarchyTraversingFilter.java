package org.springframework.core.type.filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.lang.Nullable;

import java.io.IOException;

public abstract class AbstractTypeHierarchyTraversingFilter implements TypeFilter {

    protected final Log logger = LogFactory.getLog(getClass());

    private final boolean considerInherited;

    private final boolean considerInterfaces;

    protected AbstractTypeHierarchyTraversingFilter(boolean considerInherited, boolean considerInterfaces) {
        this.considerInherited = considerInherited;
        this.considerInterfaces = considerInterfaces;
    }

    @Override
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
        // This method optimizes avoiding unnecessary creation of ClassReaders
        // as well as visiting over those readers.
        if (matchSelf(metadataReader)) {
            return true;
        }
        ClassMetadata metadata = metadataReader.getClassMetadata();
        if (matchClassName(metadata.getClassName())) {
            return true;
        }
        if (this.considerInherited) {
            String superClassName = metadata.getSuperClassName();
            if (superClassName != null) {
                // Optimization to avoid creating ClassReader for super class.
                Boolean superClassMatch = matchSuperClass(superClassName);
                if (superClassMatch != null) {
                    if (superClassMatch.booleanValue()) {
                        return true;
                    }
                } else {
                    // Need to read super class to determine a match...
                    try {
                        if (match(metadata.getSuperClassName(), metadataReaderFactory)) {
                            return true;
                        }
                    } catch (IOException ex) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Could not read super class [" + metadata.getSuperClassName() + "] of type-filtered class [" + metadata.getClassName() + "]");
                        }
                    }
                }
            }
        }
        if (this.considerInterfaces) {
            for (String ifc : metadata.getInterfaceNames()) {
                // Optimization to avoid creating ClassReader for super class
                Boolean interfaceMatch = matchInterface(ifc);
                if (interfaceMatch != null) {
                    if (interfaceMatch.booleanValue()) {
                        return true;
                    }
                } else {
                    // Need to read interface to determine a match...
                    try {
                        if (match(ifc, metadataReaderFactory)) {
                            return true;
                        }
                    } catch (IOException ex) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Could not read interface [" + ifc + "] for type-filtered class [" + metadata.getClassName() + "]");
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean match(String className, MetadataReaderFactory metadataReaderFactory) throws IOException {
        return match(metadataReaderFactory.getMetadataReader(className), metadataReaderFactory);
    }

    protected boolean matchSelf(MetadataReader metadataReader) {
        return false;
    }

    protected boolean matchClassName(String className) {
        return false;
    }

    @Nullable
    protected Boolean matchSuperClass(String superClassName) {
        return null;
    }

    @Nullable
    protected Boolean matchInterface(String interfaceName) {
        return null;
    }

}
