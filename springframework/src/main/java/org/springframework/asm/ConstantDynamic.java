package org.springframework.asm;

import java.util.Arrays;

public final class ConstantDynamic {

    private final String name;

    private final String descriptor;

    private final Handle bootstrapMethod;

    private final Object[] bootstrapMethodArguments;

    public ConstantDynamic(final String name, final String descriptor, final Handle bootstrapMethod, final Object... bootstrapMethodArguments) {
        this.name = name;
        this.descriptor = descriptor;
        this.bootstrapMethod = bootstrapMethod;
        this.bootstrapMethodArguments = bootstrapMethodArguments;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public Handle getBootstrapMethod() {
        return bootstrapMethod;
    }

    public int getBootstrapMethodArgumentCount() {
        return bootstrapMethodArguments.length;
    }

    public Object getBootstrapMethodArgument(final int index) {
        return bootstrapMethodArguments[index];
    }

    Object[] getBootstrapMethodArgumentsUnsafe() {
        return bootstrapMethodArguments;
    }

    public int getSize() {
        char firstCharOfDescriptor = descriptor.charAt(0);
        return (firstCharOfDescriptor == 'J' || firstCharOfDescriptor == 'D') ? 2 : 1;
    }

    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof ConstantDynamic)) {
            return false;
        }
        ConstantDynamic constantDynamic = (ConstantDynamic) object;
        return name.equals(constantDynamic.name) && descriptor.equals(constantDynamic.descriptor) && bootstrapMethod.equals(constantDynamic.bootstrapMethod) && Arrays.equals(bootstrapMethodArguments, constantDynamic.bootstrapMethodArguments);
    }

    @Override
    public int hashCode() {
        return name.hashCode() ^ Integer.rotateLeft(descriptor.hashCode(), 8) ^ Integer.rotateLeft(bootstrapMethod.hashCode(), 16) ^ Integer.rotateLeft(Arrays.hashCode(bootstrapMethodArguments), 24);
    }

    @Override
    public String toString() {
        return name + " : " + descriptor + ' ' + bootstrapMethod + ' ' + Arrays.toString(bootstrapMethodArguments);
    }

}
