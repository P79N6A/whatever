package org.springframework.core.io.buffer;

import org.springframework.util.Assert;

import java.nio.ByteBuffer;
import java.util.List;

public class DefaultDataBufferFactory implements DataBufferFactory {

    public static final int DEFAULT_INITIAL_CAPACITY = 256;

    private final boolean preferDirect;

    private final int defaultInitialCapacity;

    public DefaultDataBufferFactory() {
        this(false);
    }

    public DefaultDataBufferFactory(boolean preferDirect) {
        this(preferDirect, DEFAULT_INITIAL_CAPACITY);
    }

    public DefaultDataBufferFactory(boolean preferDirect, int defaultInitialCapacity) {
        Assert.isTrue(defaultInitialCapacity > 0, "'defaultInitialCapacity' should be larger than 0");
        this.preferDirect = preferDirect;
        this.defaultInitialCapacity = defaultInitialCapacity;
    }

    @Override
    public DefaultDataBuffer allocateBuffer() {
        return allocateBuffer(this.defaultInitialCapacity);
    }

    @Override
    public DefaultDataBuffer allocateBuffer(int initialCapacity) {
        ByteBuffer byteBuffer = (this.preferDirect ? ByteBuffer.allocateDirect(initialCapacity) : ByteBuffer.allocate(initialCapacity));
        return DefaultDataBuffer.fromEmptyByteBuffer(this, byteBuffer);
    }

    @Override
    public DefaultDataBuffer wrap(ByteBuffer byteBuffer) {
        return DefaultDataBuffer.fromFilledByteBuffer(this, byteBuffer.slice());
    }

    @Override
    public DefaultDataBuffer wrap(byte[] bytes) {
        return DefaultDataBuffer.fromFilledByteBuffer(this, ByteBuffer.wrap(bytes));
    }

    @Override
    public DefaultDataBuffer join(List<? extends DataBuffer> dataBuffers) {
        Assert.notEmpty(dataBuffers, "DataBuffer List must not be empty");
        int capacity = dataBuffers.stream().mapToInt(DataBuffer::readableByteCount).sum();
        DefaultDataBuffer result = allocateBuffer(capacity);
        dataBuffers.forEach(result::write);
        dataBuffers.forEach(DataBufferUtils::release);
        return result;
    }

    @Override
    public String toString() {
        return "DefaultDataBufferFactory (preferDirect=" + this.preferDirect + ")";
    }

}
