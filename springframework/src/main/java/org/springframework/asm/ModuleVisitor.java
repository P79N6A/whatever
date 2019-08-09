package org.springframework.asm;

public abstract class ModuleVisitor {

    protected final int api;

    protected ModuleVisitor mv;

    public ModuleVisitor(final int api) {
        this(api, null);
    }

    public ModuleVisitor(final int api, final ModuleVisitor moduleVisitor) {
        if (api != Opcodes.ASM7 && api != Opcodes.ASM6) {
            throw new IllegalArgumentException("Unsupported api " + api);
        }
        this.api = api;
        this.mv = moduleVisitor;
    }

    public void visitMainClass(final String mainClass) {
        if (mv != null) {
            mv.visitMainClass(mainClass);
        }
    }

    public void visitPackage(final String packaze) {
        if (mv != null) {
            mv.visitPackage(packaze);
        }
    }

    public void visitRequire(final String module, final int access, final String version) {
        if (mv != null) {
            mv.visitRequire(module, access, version);
        }
    }

    public void visitExport(final String packaze, final int access, final String... modules) {
        if (mv != null) {
            mv.visitExport(packaze, access, modules);
        }
    }

    public void visitOpen(final String packaze, final int access, final String... modules) {
        if (mv != null) {
            mv.visitOpen(packaze, access, modules);
        }
    }

    public void visitUse(final String service) {
        if (mv != null) {
            mv.visitUse(service);
        }
    }

    public void visitProvide(final String service, final String... providers) {
        if (mv != null) {
            mv.visitProvide(service, providers);
        }
    }

    public void visitEnd() {
        if (mv != null) {
            mv.visitEnd();
        }
    }

}
