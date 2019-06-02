package io.netty.buffer;

import java.nio.ByteOrder;

final class UnreleasableByteBuf extends WrappedByteBuf {

    private SwappedByteBuf swappedBuf;

    UnreleasableByteBuf(ByteBuf buf) {
        super(buf instanceof UnreleasableByteBuf ? buf.unwrap() : buf);
    }

    @Override
    public ByteBuf order(ByteOrder endianness) {
        if (endianness == null) {
            throw new NullPointerException("endianness");
        }
        if (endianness == order()) {
            return this;
        }

        SwappedByteBuf swappedBuf = this.swappedBuf;
        if (swappedBuf == null) {
            this.swappedBuf = swappedBuf = new SwappedByteBuf(this);
        }
        return swappedBuf;
    }

    @Override
    public ByteBuf asReadOnly() {
        return buf.isReadOnly() ? this : new UnreleasableByteBuf(buf.asReadOnly());
    }

    @Override
    public ByteBuf readSlice(int length) {
        return new UnreleasableByteBuf(buf.readSlice(length));
    }

    @Override
    public ByteBuf readRetainedSlice(int length) {

        return readSlice(length);
    }

    @Override
    public ByteBuf slice() {
        return new UnreleasableByteBuf(buf.slice());
    }

    @Override
    public ByteBuf retainedSlice() {

        return slice();
    }

    @Override
    public ByteBuf slice(int index, int length) {
        return new UnreleasableByteBuf(buf.slice(index, length));
    }

    @Override
    public ByteBuf retainedSlice(int index, int length) {

        return slice(index, length);
    }

    @Override
    public ByteBuf duplicate() {
        return new UnreleasableByteBuf(buf.duplicate());
    }

    @Override
    public ByteBuf retainedDuplicate() {

        return duplicate();
    }

    @Override
    public ByteBuf retain(int increment) {
        return this;
    }

    @Override
    public ByteBuf retain() {
        return this;
    }

    @Override
    public ByteBuf touch() {
        return this;
    }

    @Override
    public ByteBuf touch(Object hint) {
        return this;
    }

    @Override
    public boolean release() {
        return false;
    }

    @Override
    public boolean release(int decrement) {
        return false;
    }
}
