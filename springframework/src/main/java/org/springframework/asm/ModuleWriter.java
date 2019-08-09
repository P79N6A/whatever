package org.springframework.asm;

final class ModuleWriter extends ModuleVisitor {

    private final SymbolTable symbolTable;

    private final int moduleNameIndex;

    private final int moduleFlags;

    private final int moduleVersionIndex;

    private int requiresCount;

    private final ByteVector requires;

    private int exportsCount;

    private final ByteVector exports;

    private int opensCount;

    private final ByteVector opens;

    private int usesCount;

    private final ByteVector usesIndex;

    private int providesCount;

    private final ByteVector provides;

    private int packageCount;

    private final ByteVector packageIndex;

    private int mainClassIndex;

    ModuleWriter(final SymbolTable symbolTable, final int name, final int access, final int version) {
        super(Opcodes.ASM7);
        this.symbolTable = symbolTable;
        this.moduleNameIndex = name;
        this.moduleFlags = access;
        this.moduleVersionIndex = version;
        this.requires = new ByteVector();
        this.exports = new ByteVector();
        this.opens = new ByteVector();
        this.usesIndex = new ByteVector();
        this.provides = new ByteVector();
        this.packageIndex = new ByteVector();
    }

    @Override
    public void visitMainClass(final String mainClass) {
        this.mainClassIndex = symbolTable.addConstantClass(mainClass).index;
    }

    @Override
    public void visitPackage(final String packaze) {
        packageIndex.putShort(symbolTable.addConstantPackage(packaze).index);
        packageCount++;
    }

    @Override
    public void visitRequire(final String module, final int access, final String version) {
        requires.putShort(symbolTable.addConstantModule(module).index).putShort(access).putShort(version == null ? 0 : symbolTable.addConstantUtf8(version));
        requiresCount++;
    }

    @Override
    public void visitExport(final String packaze, final int access, final String... modules) {
        exports.putShort(symbolTable.addConstantPackage(packaze).index).putShort(access);
        if (modules == null) {
            exports.putShort(0);
        } else {
            exports.putShort(modules.length);
            for (String module : modules) {
                exports.putShort(symbolTable.addConstantModule(module).index);
            }
        }
        exportsCount++;
    }

    @Override
    public void visitOpen(final String packaze, final int access, final String... modules) {
        opens.putShort(symbolTable.addConstantPackage(packaze).index).putShort(access);
        if (modules == null) {
            opens.putShort(0);
        } else {
            opens.putShort(modules.length);
            for (String module : modules) {
                opens.putShort(symbolTable.addConstantModule(module).index);
            }
        }
        opensCount++;
    }

    @Override
    public void visitUse(final String service) {
        usesIndex.putShort(symbolTable.addConstantClass(service).index);
        usesCount++;
    }

    @Override
    public void visitProvide(final String service, final String... providers) {
        provides.putShort(symbolTable.addConstantClass(service).index);
        provides.putShort(providers.length);
        for (String provider : providers) {
            provides.putShort(symbolTable.addConstantClass(provider).index);
        }
        providesCount++;
    }

    @Override
    public void visitEnd() {
        // Nothing to do.
    }

    int getAttributeCount() {
        return 1 + (packageCount > 0 ? 1 : 0) + (mainClassIndex > 0 ? 1 : 0);
    }

    int computeAttributesSize() {
        symbolTable.addConstantUtf8(Constants.MODULE);
        // 6 attribute header bytes, 6 bytes for name, flags and version, and 5 * 2 bytes for counts.
        int size = 22 + requires.length + exports.length + opens.length + usesIndex.length + provides.length;
        if (packageCount > 0) {
            symbolTable.addConstantUtf8(Constants.MODULE_PACKAGES);
            // 6 attribute header bytes, and 2 bytes for package_count.
            size += 8 + packageIndex.length;
        }
        if (mainClassIndex > 0) {
            symbolTable.addConstantUtf8(Constants.MODULE_MAIN_CLASS);
            // 6 attribute header bytes, and 2 bytes for main_class_index.
            size += 8;
        }
        return size;
    }

    void putAttributes(final ByteVector output) {
        // 6 bytes for name, flags and version, and 5 * 2 bytes for counts.
        int moduleAttributeLength = 16 + requires.length + exports.length + opens.length + usesIndex.length + provides.length;
        output.putShort(symbolTable.addConstantUtf8(Constants.MODULE)).putInt(moduleAttributeLength).putShort(moduleNameIndex).putShort(moduleFlags).putShort(moduleVersionIndex).putShort(requiresCount).putByteArray(requires.data, 0, requires.length).putShort(exportsCount).putByteArray(exports.data, 0, exports.length).putShort(opensCount).putByteArray(opens.data, 0, opens.length).putShort(usesCount).putByteArray(usesIndex.data, 0, usesIndex.length).putShort(providesCount).putByteArray(provides.data, 0, provides.length);
        if (packageCount > 0) {
            output.putShort(symbolTable.addConstantUtf8(Constants.MODULE_PACKAGES)).putInt(2 + packageIndex.length).putShort(packageCount).putByteArray(packageIndex.data, 0, packageIndex.length);
        }
        if (mainClassIndex > 0) {
            output.putShort(symbolTable.addConstantUtf8(Constants.MODULE_MAIN_CLASS)).putInt(2).putShort(mainClassIndex);
        }
    }

}
