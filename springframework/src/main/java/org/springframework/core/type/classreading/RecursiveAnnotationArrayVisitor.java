package org.springframework.core.type.classreading;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.Type;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

@Deprecated
class RecursiveAnnotationArrayVisitor extends AbstractRecursiveAnnotationVisitor {

    private final String attributeName;

    private final List<AnnotationAttributes> allNestedAttributes = new ArrayList<>();

    public RecursiveAnnotationArrayVisitor(String attributeName, AnnotationAttributes attributes, @Nullable ClassLoader classLoader) {
        super(classLoader, attributes);
        this.attributeName = attributeName;
    }

    @Override
    public void visit(String attributeName, Object attributeValue) {
        Object newValue = attributeValue;
        Object existingValue = this.attributes.get(this.attributeName);
        if (existingValue != null) {
            newValue = ObjectUtils.addObjectToArray((Object[]) existingValue, newValue);
        } else {
            Class<?> arrayClass = newValue.getClass();
            if (Enum.class.isAssignableFrom(arrayClass)) {
                while (arrayClass.getSuperclass() != null && !arrayClass.isEnum()) {
                    arrayClass = arrayClass.getSuperclass();
                }
            }
            Object[] newArray = (Object[]) Array.newInstance(arrayClass, 1);
            newArray[0] = newValue;
            newValue = newArray;
        }
        this.attributes.put(this.attributeName, newValue);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String attributeName, String asmTypeDescriptor) {
        String annotationType = Type.getType(asmTypeDescriptor).getClassName();
        AnnotationAttributes nestedAttributes = new AnnotationAttributes(annotationType, this.classLoader);
        this.allNestedAttributes.add(nestedAttributes);
        return new RecursiveAnnotationAttributesVisitor(annotationType, nestedAttributes, this.classLoader);
    }

    @Override
    public void visitEnd() {
        if (!this.allNestedAttributes.isEmpty()) {
            this.attributes.put(this.attributeName, this.allNestedAttributes.toArray(new AnnotationAttributes[0]));
        } else if (!this.attributes.containsKey(this.attributeName)) {
            Class<? extends Annotation> annotationType = this.attributes.annotationType();
            if (annotationType != null) {
                try {
                    Class<?> attributeType = annotationType.getMethod(this.attributeName).getReturnType();
                    if (attributeType.isArray()) {
                        Class<?> elementType = attributeType.getComponentType();
                        if (elementType.isAnnotation()) {
                            elementType = AnnotationAttributes.class;
                        }
                        this.attributes.put(this.attributeName, Array.newInstance(elementType, 0));
                    }
                } catch (NoSuchMethodException ex) {
                    // Corresponding attribute method not found: cannot expose empty array.
                }
            }
        }
    }

}
