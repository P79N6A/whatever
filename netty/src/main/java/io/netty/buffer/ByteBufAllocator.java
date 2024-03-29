package io.netty.buffer;

public interface ByteBufAllocator {

    ByteBufAllocator DEFAULT = ByteBufUtil.DEFAULT_ALLOCATOR;

    ByteBuf buffer();

    ByteBuf buffer(int initialCapacity);

    ByteBuf buffer(int initialCapacity, int maxCapacity);

    ByteBuf ioBuffer();

    ByteBuf ioBuffer(int initialCapacity);

    ByteBuf ioBuffer(int initialCapacity, int maxCapacity);

    ByteBuf heapBuffer();

    ByteBuf heapBuffer(int initialCapacity);

    ByteBuf heapBuffer(int initialCapacity, int maxCapacity);

    ByteBuf directBuffer();

    ByteBuf directBuffer(int initialCapacity);

    ByteBuf directBuffer(int initialCapacity, int maxCapacity);

    CompositeByteBuf compositeBuffer();

    CompositeByteBuf compositeBuffer(int maxNumComponents);

    CompositeByteBuf compositeHeapBuffer();

    CompositeByteBuf compositeHeapBuffer(int maxNumComponents);

    CompositeByteBuf compositeDirectBuffer();

    CompositeByteBuf compositeDirectBuffer(int maxNumComponents);

    boolean isDirectBufferPooled();

    int calculateNewCapacity(int minNewCapacity, int maxCapacity);
}
