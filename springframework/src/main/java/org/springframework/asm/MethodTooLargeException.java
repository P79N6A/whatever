package org.springframework.asm;

public final class MethodTooLargeException extends IndexOutOfBoundsException {
    private static final long serialVersionUID = 6807380416709738314L;

    private final String className;

    private final String methodName;

    private final String descriptor;

    private final int codeSize;

    public MethodTooLargeException(final String className, final String methodName, final String descriptor, final int codeSize) {
        super("Method too large: " + className + "." + methodName + " " + descriptor);
        this.className = className;
        this.methodName = methodName;
        this.descriptor = descriptor;
        this.codeSize = codeSize;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public int getCodeSize() {
        return codeSize;
    }

}
