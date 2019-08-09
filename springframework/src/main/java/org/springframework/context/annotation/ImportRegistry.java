package org.springframework.context.annotation;

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;

interface ImportRegistry {

    @Nullable
    AnnotationMetadata getImportingClassFor(String importedClass);

    void removeImportingClass(String importingClass);

}
