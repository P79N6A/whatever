package org.springframework.asm;

public abstract class ClassVisitor {

    protected final int api;

    protected ClassVisitor cv;

    public ClassVisitor(final int api) {
        this(api, null);
    }

    public ClassVisitor(final int api, final ClassVisitor classVisitor) {
        if (api != Opcodes.ASM7 && api != Opcodes.ASM6 && api != Opcodes.ASM5 && api != Opcodes.ASM4) {
            throw new IllegalArgumentException("Unsupported api " + api);
        }
        this.api = api;
        this.cv = classVisitor;
    }

    public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
        if (cv != null) {
            cv.visit(version, access, name, signature, superName, interfaces);
        }
    }

    public void visitSource(final String source, final String debug) {
        if (cv != null) {
            cv.visitSource(source, debug);
        }
    }

    public ModuleVisitor visitModule(final String name, final int access, final String version) {
        if (api < Opcodes.ASM6) {
            throw new UnsupportedOperationException("This feature requires ASM6");
        }
        if (cv != null) {
            return cv.visitModule(name, access, version);
        }
        return null;
    }

    public void visitNestHost(final String nestHost) {
        if (api < Opcodes.ASM7) {
            throw new UnsupportedOperationException("This feature requires ASM7");
        }
        if (cv != null) {
            cv.visitNestHost(nestHost);
        }
    }

    public void visitOuterClass(final String owner, final String name, final String descriptor) {
        if (cv != null) {
            cv.visitOuterClass(owner, name, descriptor);
        }
    }

    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
        if (cv != null) {
            return cv.visitAnnotation(descriptor, visible);
        }
        return null;
    }

    public AnnotationVisitor visitTypeAnnotation(final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        if (api < Opcodes.ASM5) {
            throw new UnsupportedOperationException("This feature requires ASM5");
        }
        if (cv != null) {
            return cv.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
        }
        return null;
    }

    public void visitAttribute(final Attribute attribute) {
        if (cv != null) {
            cv.visitAttribute(attribute);
        }
    }

    public void visitNestMember(final String nestMember) {
        if (api < Opcodes.ASM7) {
            throw new UnsupportedOperationException("This feature requires ASM7");
        }
        if (cv != null) {
            cv.visitNestMember(nestMember);
        }
    }

    public void visitInnerClass(final String name, final String outerName, final String innerName, final int access) {
        if (cv != null) {
            cv.visitInnerClass(name, outerName, innerName, access);
        }
    }

    public FieldVisitor visitField(final int access, final String name, final String descriptor, final String signature, final Object value) {
        if (cv != null) {
            return cv.visitField(access, name, descriptor, signature, value);
        }
        return null;
    }

    public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
        if (cv != null) {
            return cv.visitMethod(access, name, descriptor, signature, exceptions);
        }
        return null;
    }

    public void visitEnd() {
        if (cv != null) {
            cv.visitEnd();
        }
    }

}
