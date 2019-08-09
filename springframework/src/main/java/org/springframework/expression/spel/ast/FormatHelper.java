package org.springframework.expression.spel.ast;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import java.util.List;
import java.util.StringJoiner;

abstract class FormatHelper {

    public static String formatMethodForMessage(String name, List<TypeDescriptor> argumentTypes) {
        StringJoiner sj = new StringJoiner(",", "(", ")");
        for (TypeDescriptor typeDescriptor : argumentTypes) {
            if (typeDescriptor != null) {
                sj.add(formatClassNameForMessage(typeDescriptor.getType()));
            } else {
                sj.add(formatClassNameForMessage(null));
            }
        }
        return name + sj.toString();
    }

    public static String formatClassNameForMessage(@Nullable Class<?> clazz) {
        return (clazz != null ? ClassUtils.getQualifiedName(clazz) : "null");
    }

}
