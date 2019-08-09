package org.springframework.core.type.classreading;

import org.springframework.asm.Opcodes;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.MethodMetadata;

final class SimpleMethodMetadata implements MethodMetadata {

    private final String methodName;

    private final int access;

    private final String declaringClassName;

    private final String returnTypeName;

    private final MergedAnnotations annotations;

    public SimpleMethodMetadata(String methodName, int access, String declaringClassName, String returnTypeName, MergedAnnotations annotations) {
        this.methodName = methodName;
        this.access = access;
        this.declaringClassName = declaringClassName;
        this.returnTypeName = returnTypeName;
        this.annotations = annotations;
    }

    @Override
    public String getMethodName() {
        return this.methodName;
    }

    @Override
    public String getDeclaringClassName() {
        return this.declaringClassName;
    }

    @Override
    public String getReturnTypeName() {
        return this.returnTypeName;
    }

    @Override
    public boolean isAbstract() {
        return (this.access & Opcodes.ACC_ABSTRACT) != 0;
    }

    @Override
    public boolean isStatic() {
        return (this.access & Opcodes.ACC_STATIC) != 0;
    }

    @Override
    public boolean isFinal() {
        return (this.access & Opcodes.ACC_FINAL) != 0;
    }

    @Override
    public boolean isOverridable() {
        return !isStatic() && !isFinal() && !isPrivate();
    }

    public boolean isPrivate() {
        return (this.access & Opcodes.ACC_PRIVATE) != 0;
    }

    @Override
    public MergedAnnotations getAnnotations() {
        return this.annotations;
    }

}
