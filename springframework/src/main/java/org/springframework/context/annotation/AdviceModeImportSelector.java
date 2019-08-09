package org.springframework.context.annotation;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;

public abstract class AdviceModeImportSelector<A extends Annotation> implements ImportSelector {

    public static final String DEFAULT_ADVICE_MODE_ATTRIBUTE_NAME = "mode";

    protected String getAdviceModeAttributeName() {
        return DEFAULT_ADVICE_MODE_ATTRIBUTE_NAME;
    }

    @Override
    public final String[] selectImports(AnnotationMetadata importingClassMetadata) {
        Class<?> annType = GenericTypeResolver.resolveTypeArgument(getClass(), AdviceModeImportSelector.class);
        Assert.state(annType != null, "Unresolvable type argument for AdviceModeImportSelector");
        AnnotationAttributes attributes = AnnotationConfigUtils.attributesFor(importingClassMetadata, annType);
        if (attributes == null) {
            throw new IllegalArgumentException(String.format("@%s is not present on importing class '%s' as expected", annType.getSimpleName(), importingClassMetadata.getClassName()));
        }
        AdviceMode adviceMode = attributes.getEnum(getAdviceModeAttributeName());
        String[] imports = selectImports(adviceMode);
        if (imports == null) {
            throw new IllegalArgumentException("Unknown AdviceMode: " + adviceMode);
        }
        return imports;
    }

    @Nullable
    protected abstract String[] selectImports(AdviceMode adviceMode);

}
