package org.springframework.context.annotation;

import org.springframework.core.type.AnnotationMetadata;

public interface ImportSelector {

    String[] selectImports(AnnotationMetadata importingClassMetadata);

}
