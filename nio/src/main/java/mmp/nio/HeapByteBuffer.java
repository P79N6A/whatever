package mmp.nio;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

class HeapByteBuffer extends ByteBuffer {

    HeapByteBuffer(int cap, int lim) {

        super(-1, 0, lim, cap, new byte[cap], 0);

    }

    HeapByteBuffer(byte[] buf, int off, int len) {

        super(-1, off, off + len, buf.length, buf, 0);

    }

    protected HeapByteBuffer(byte[] buf, int mark, int pos, int lim, int cap, int off) {

        super(mark, pos, lim, cap, buf, off);

    }

    public ByteBuffer slice() {
        return new HeapByteBuffer(hb, -1, 0, this.remaining(), this.remaining(), this.position() + offset);
    }

    public ByteBuffer duplicate() {
        return new HeapByteBuffer(hb, this.markValue(), this.position(), this.limit(), this.capacity(), offset);
    }

    protected int ix(int i) {
        return i + offset;
    }

    public byte get() {
        return hb[ix(nextGetIndex())];
    }

    public byte get(int i) {
        return hb[ix(checkIndex(i))];
    }

    public ByteBuffer get(byte[] dst, int offset, int length) {
        checkBounds(offset, length, dst.length);
        if (length > remaining())
            throw new BufferUnderflowException();
        System.arraycopy(hb, ix(position()), dst, offset, length);
        position(position() + length);
        return this;
    }

    public boolean isDirect() {
        return false;
    }

    public boolean isReadOnly() {
        return false;
    }

    public ByteBuffer put(byte x) {

        hb[ix(nextPutIndex())] = x;
        return this;

    }

    public ByteBuffer put(int i, byte x) {

        hb[ix(checkIndex(i))] = x;
        return this;

    }

    public ByteBuffer put(byte[] src, int offset, int length) {

        checkBounds(offset, length, src.length);
        if (length > remaining())
            throw new BufferOverflowException();
        System.arraycopy(src, offset, hb, ix(position()), length);
        position(position() + length);
        return this;

    }

    public ByteBuffer put(ByteBuffer src) {

        if (src instanceof HeapByteBuffer) {
            if (src == this)
                throw new IllegalArgumentException();
            HeapByteBuffer sb = (HeapByteBuffer) src;
            int n = sb.remaining();
            if (n > remaining())
                throw new BufferOverflowException();
            System.arraycopy(sb.hb, sb.ix(sb.position()), hb, ix(position()), n);
            sb.position(sb.position() + n);
            position(position() + n);
        } else if (src.isDirect()) {
            int n = src.remaining();
            if (n > remaining())
                throw new BufferOverflowException();
            src.get(hb, ix(position()), n);
            position(position() + n);
        } else {
            super.put(src);
        }
        return this;

    }

    // 压缩此缓冲区
    // 将缓冲区的当前位置和界限之间的字节（如果有）复制到缓冲区的开始处（覆盖，没覆盖的位置数据还在）
    // 然后将缓冲区的位置设置为remaining，并将其界限设置为其容量，如果已定义了标记，则丢弃它
    // 将缓冲区的位置设置为复制的字节数，而不是零，以调用此方法后可以紧接着调用另一个相对put方法
    // 从缓冲区写入数据之后调用此方法，以防写入不完整
    public ByteBuffer compact() {
        // 源数组 - position + offset - 源数组 - offset - 原剩余的长度
        System.arraycopy(hb, ix(position()), hb, ix(0), remaining());
        // 重置position为原剩余的长度
        position(remaining());
        // 重置limit
        limit(capacity());
        // 重置标记
        discardMark();
        return this;

    }

    byte _get(int i) {
        return hb[i];
    }

    void _put(int i, byte b) {

        hb[i] = b;

    }

    public char getChar() {
        return Bits.getChar(this, ix(nextGetIndex(2)), bigEndian);
    }

    public char getChar(int i) {
        return Bits.getChar(this, ix(checkIndex(i, 2)), bigEndian);
    }

    public ByteBuffer putChar(char x) {

        Bits.putChar(this, ix(nextPutIndex(2)), x, bigEndian);
        return this;

    }

    public ByteBuffer putChar(int i, char x) {

        Bits.putChar(this, ix(checkIndex(i, 2)), x, bigEndian);
        return this;

    }

    public short getShort() {
        return Bits.getShort(this, ix(nextGetIndex(2)), bigEndian);
    }

    public short getShort(int i) {
        return Bits.getShort(this, ix(checkIndex(i, 2)), bigEndian);
    }

    public ByteBuffer putShort(short x) {

        Bits.putShort(this, ix(nextPutIndex(2)), x, bigEndian);
        return this;

    }

    public ByteBuffer putShort(int i, short x) {

        Bits.putShort(this, ix(checkIndex(i, 2)), x, bigEndian);
        return this;

    }

    public int getInt() {
        return Bits.getInt(this, ix(nextGetIndex(4)), bigEndian);
    }

    public int getInt(int i) {
        return Bits.getInt(this, ix(checkIndex(i, 4)), bigEndian);
    }

    public ByteBuffer putInt(int x) {

        Bits.putInt(this, ix(nextPutIndex(4)), x, bigEndian);
        return this;

    }

    public ByteBuffer putInt(int i, int x) {

        Bits.putInt(this, ix(checkIndex(i, 4)), x, bigEndian);
        return this;

    }

    public long getLong() {
        return Bits.getLong(this, ix(nextGetIndex(8)), bigEndian);
    }

    public long getLong(int i) {
        return Bits.getLong(this, ix(checkIndex(i, 8)), bigEndian);
    }

    public ByteBuffer putLong(long x) {

        Bits.putLong(this, ix(nextPutIndex(8)), x, bigEndian);
        return this;

    }

    public ByteBuffer putLong(int i, long x) {

        Bits.putLong(this, ix(checkIndex(i, 8)), x, bigEndian);
        return this;

    }

    public float getFloat() {
        return Bits.getFloat(this, ix(nextGetIndex(4)), bigEndian);
    }

    public float getFloat(int i) {
        return Bits.getFloat(this, ix(checkIndex(i, 4)), bigEndian);
    }

    public ByteBuffer putFloat(float x) {

        Bits.putFloat(this, ix(nextPutIndex(4)), x, bigEndian);
        return this;

    }

    public ByteBuffer putFloat(int i, float x) {

        Bits.putFloat(this, ix(checkIndex(i, 4)), x, bigEndian);
        return this;

    }

    public double getDouble() {
        return Bits.getDouble(this, ix(nextGetIndex(8)), bigEndian);
    }

    public double getDouble(int i) {
        return Bits.getDouble(this, ix(checkIndex(i, 8)), bigEndian);
    }

    public ByteBuffer putDouble(double x) {

        Bits.putDouble(this, ix(nextPutIndex(8)), x, bigEndian);
        return this;

    }

    public ByteBuffer putDouble(int i, double x) {

        Bits.putDouble(this, ix(checkIndex(i, 8)), x, bigEndian);
        return this;

    }

}
