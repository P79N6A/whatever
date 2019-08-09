package org.springframework.asm;

public final class Handle {

    private final int tag;

    private final String owner;

    private final String name;

    private final String descriptor;

    private final boolean isInterface;

    @Deprecated
    public Handle(final int tag, final String owner, final String name, final String descriptor) {
        this(tag, owner, name, descriptor, tag == Opcodes.H_INVOKEINTERFACE);
    }

    public Handle(final int tag, final String owner, final String name, final String descriptor, final boolean isInterface) {
        this.tag = tag;
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
        this.isInterface = isInterface;
    }

    public int getTag() {
        return tag;
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return descriptor;
    }

    public boolean isInterface() {
        return isInterface;
    }

    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Handle)) {
            return false;
        }
        Handle handle = (Handle) object;
        return tag == handle.tag && isInterface == handle.isInterface && owner.equals(handle.owner) && name.equals(handle.name) && descriptor.equals(handle.descriptor);
    }

    @Override
    public int hashCode() {
        return tag + (isInterface ? 64 : 0) + owner.hashCode() * name.hashCode() * descriptor.hashCode();
    }

    @Override
    public String toString() {
        return owner + '.' + name + descriptor + " (" + tag + (isInterface ? " itf" : "") + ')';
    }

}
