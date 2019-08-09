package org.springframework.asm;

public abstract class FieldVisitor {

    protected final int api;

    protected FieldVisitor fv;

    public FieldVisitor(final int api) {
        this(api, null);
    }

    public FieldVisitor(final int api, final FieldVisitor fieldVisitor) {
        if (api != Opcodes.ASM7 && api != Opcodes.ASM6 && api != Opcodes.ASM5 && api != Opcodes.ASM4) {
            throw new IllegalArgumentException("Unsupported api " + api);
        }
        this.api = api;
        this.fv = fieldVisitor;
    }

    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
        if (fv != null) {
            return fv.visitAnnotation(descriptor, visible);
        }
        return null;
    }

    public AnnotationVisitor visitTypeAnnotation(final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        if (api < Opcodes.ASM5) {
            throw new UnsupportedOperationException("This feature requires ASM5");
        }
        if (fv != null) {
            return fv.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
        }
        return null;
    }

    public void visitAttribute(final Attribute attribute) {
        if (fv != null) {
            fv.visitAttribute(attribute);
        }
    }

    public void visitEnd() {
        if (fv != null) {
            fv.visitEnd();
        }
    }

}
