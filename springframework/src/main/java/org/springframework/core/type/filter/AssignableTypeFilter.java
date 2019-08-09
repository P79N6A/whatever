package org.springframework.core.type.filter;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

public class AssignableTypeFilter extends AbstractTypeHierarchyTraversingFilter {

    private final Class<?> targetType;

    public AssignableTypeFilter(Class<?> targetType) {
        super(true, true);
        this.targetType = targetType;
    }

    public final Class<?> getTargetType() {
        return this.targetType;
    }

    @Override
    protected boolean matchClassName(String className) {
        return this.targetType.getName().equals(className);
    }

    @Override
    @Nullable
    protected Boolean matchSuperClass(String superClassName) {
        return matchTargetType(superClassName);
    }

    @Override
    @Nullable
    protected Boolean matchInterface(String interfaceName) {
        return matchTargetType(interfaceName);
    }

    @Nullable
    protected Boolean matchTargetType(String typeName) {
        if (this.targetType.getName().equals(typeName)) {
            return true;
        } else if (Object.class.getName().equals(typeName)) {
            return false;
        } else if (typeName.startsWith("java")) {
            try {
                Class<?> clazz = ClassUtils.forName(typeName, getClass().getClassLoader());
                return this.targetType.isAssignableFrom(clazz);
            } catch (Throwable ex) {
                // Class not regularly loadable - can't determine a match that way.
            }
        }
        return null;
    }

}
