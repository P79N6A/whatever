package io.netty.buffer;

import io.netty.util.Recycler;
import io.netty.util.Recycler.Handle;
import io.netty.util.internal.PlatformDependent;

final class PooledUnsafeHeapByteBuf extends PooledHeapByteBuf {

    private static final Recycler<PooledUnsafeHeapByteBuf> RECYCLER = new Recycler<PooledUnsafeHeapByteBuf>() {
        @Override
        protected PooledUnsafeHeapByteBuf newObject(Handle<PooledUnsafeHeapByteBuf> handle) {
            return new PooledUnsafeHeapByteBuf(handle, 0);
        }
    };

    private PooledUnsafeHeapByteBuf(Handle<PooledUnsafeHeapByteBuf> recyclerHandle, int maxCapacity) {
        super(recyclerHandle, maxCapacity);
    }

    static PooledUnsafeHeapByteBuf newUnsafeInstance(int maxCapacity) {
        PooledUnsafeHeapByteBuf buf = RECYCLER.get();
        buf.reuse(maxCapacity);
        return buf;
    }

    @Override
    protected byte _getByte(int index) {
        return UnsafeByteBufUtil.getByte(memory, idx(index));
    }

    @Override
    protected short _getShort(int index) {
        return UnsafeByteBufUtil.getShort(memory, idx(index));
    }

    @Override
    protected short _getShortLE(int index) {
        return UnsafeByteBufUtil.getShortLE(memory, idx(index));
    }

    @Override
    protected int _getUnsignedMedium(int index) {
        return UnsafeByteBufUtil.getUnsignedMedium(memory, idx(index));
    }

    @Override
    protected int _getUnsignedMediumLE(int index) {
        return UnsafeByteBufUtil.getUnsignedMediumLE(memory, idx(index));
    }

    @Override
    protected int _getInt(int index) {
        return UnsafeByteBufUtil.getInt(memory, idx(index));
    }

    @Override
    protected int _getIntLE(int index) {
        return UnsafeByteBufUtil.getIntLE(memory, idx(index));
    }

    @Override
    protected long _getLong(int index) {
        return UnsafeByteBufUtil.getLong(memory, idx(index));
    }

    @Override
    protected long _getLongLE(int index) {
        return UnsafeByteBufUtil.getLongLE(memory, idx(index));
    }

    @Override
    protected void _setByte(int index, int value) {
        UnsafeByteBufUtil.setByte(memory, idx(index), value);
    }

    @Override
    protected void _setShort(int index, int value) {
        UnsafeByteBufUtil.setShort(memory, idx(index), value);
    }

    @Override
    protected void _setShortLE(int index, int value) {
        UnsafeByteBufUtil.setShortLE(memory, idx(index), value);
    }

    @Override
    protected void _setMedium(int index, int value) {
        UnsafeByteBufUtil.setMedium(memory, idx(index), value);
    }

    @Override
    protected void _setMediumLE(int index, int value) {
        UnsafeByteBufUtil.setMediumLE(memory, idx(index), value);
    }

    @Override
    protected void _setInt(int index, int value) {
        UnsafeByteBufUtil.setInt(memory, idx(index), value);
    }

    @Override
    protected void _setIntLE(int index, int value) {
        UnsafeByteBufUtil.setIntLE(memory, idx(index), value);
    }

    @Override
    protected void _setLong(int index, long value) {
        UnsafeByteBufUtil.setLong(memory, idx(index), value);
    }

    @Override
    protected void _setLongLE(int index, long value) {
        UnsafeByteBufUtil.setLongLE(memory, idx(index), value);
    }

    @Override
    public ByteBuf setZero(int index, int length) {
        if (PlatformDependent.javaVersion() >= 7) {
            checkIndex(index, length);

            UnsafeByteBufUtil.setZero(memory, idx(index), length);
            return this;
        }
        return super.setZero(index, length);
    }

    @Override
    public ByteBuf writeZero(int length) {
        if (PlatformDependent.javaVersion() >= 7) {

            ensureWritable(length);
            int wIndex = writerIndex;
            UnsafeByteBufUtil.setZero(memory, idx(wIndex), length);
            writerIndex = wIndex + length;
            return this;
        }
        return super.writeZero(length);
    }

    @Override
    @Deprecated
    protected SwappedByteBuf newSwappedByteBuf() {
        if (PlatformDependent.isUnaligned()) {

            return new UnsafeHeapSwappedByteBuf(this);
        }
        return super.newSwappedByteBuf();
    }
}
