package mmp.nio;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ReadOnlyBufferException;

public abstract class ByteBuffer extends Buffer implements Comparable<ByteBuffer> {

    /**
     * 堆缓冲
     */
    final byte[] hb;
    final int offset;
    boolean isReadOnly;

    ByteBuffer(int mark, int pos, int lim, int cap, byte[] hb, int offset) {
        super(mark, pos, lim, cap);
        this.hb = hb;
        this.offset = offset;
    }

    ByteBuffer(int mark, int pos, int lim, int cap) {
        this(mark, pos, lim, cap, null, 0);
    }

    /**
     * 分配直接缓冲
     */
    public static ByteBuffer allocateDirect(int capacity) {
        return new DirectByteBuffer(capacity);
    }

    /**
     * 创建缓冲区
     */
    public static ByteBuffer allocate(int capacity) {
        if (capacity < 0)
            throw new IllegalArgumentException();
        // 默认堆缓冲
        return new HeapByteBuffer(capacity, capacity);
    }

    /**
     * 返回一个新的缓冲，但是array是同一个
     */
    public static ByteBuffer wrap(byte[] array, int offset, int length) {
        try {
            return new HeapByteBuffer(array, offset, length);
        } catch (IllegalArgumentException x) {
            throw new IndexOutOfBoundsException();
        }
    }

    public static ByteBuffer wrap(byte[] array) {
        return wrap(array, 0, array.length);
    }

    /**
     * 子缓冲区
     */
    public abstract ByteBuffer slice();

    public abstract ByteBuffer duplicate();

    // public abstract ByteBuffer asReadOnlyBuffer();

    /**
     * 获取元素，position++
     */
    public abstract byte get();

    /**
     * 存放元素，position++
     */
    public abstract ByteBuffer put(byte b);

    public abstract byte get(int index);

    public abstract ByteBuffer put(int index, byte b);

    public ByteBuffer get(byte[] dst, int offset, int length) {
        checkBounds(offset, length, dst.length);
        if (length > remaining())
            throw new BufferUnderflowException();
        int end = offset + length;
        for (int i = offset; i < end; i++)
            dst[i] = get();
        return this;
    }

    public ByteBuffer get(byte[] dst) {
        return get(dst, 0, dst.length);
    }

    public ByteBuffer put(ByteBuffer src) {
        if (src == this)
            throw new IllegalArgumentException();
        if (isReadOnly())
            throw new ReadOnlyBufferException();
        int n = src.remaining();
        if (n > remaining())
            throw new BufferOverflowException();
        for (int i = 0; i < n; i++)
            put(src.get());
        return this;
    }

    public ByteBuffer put(byte[] src, int offset, int length) {
        // 起始偏移 长度 数组长度 加入后是否越界
        checkBounds(offset, length, src.length);
        if (length > remaining())
            throw new BufferOverflowException();
        int end = offset + length;
        for (int i = offset; i < end; i++)
            this.put(src[i]);
        return this;
    }

    public final ByteBuffer put(byte[] src) {
        return put(src, 0, src.length);
    }

    public final boolean hasArray() {
        // 堆数组 && 不是只读
        return (hb != null) && !isReadOnly;
    }

    public final byte[] array() {
        if (hb == null)
            throw new UnsupportedOperationException();
        if (isReadOnly)
            throw new ReadOnlyBufferException();
        return hb;
    }

    public final int arrayOffset() {
        if (hb == null)
            throw new UnsupportedOperationException();
        if (isReadOnly)
            throw new ReadOnlyBufferException();
        return offset;
    }

    //
    public abstract ByteBuffer compact();

    public abstract boolean isDirect();

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName());
        sb.append("[pos=");
        sb.append(position());
        sb.append(" lim=");
        sb.append(limit());
        sb.append(" cap=");
        sb.append(capacity());
        sb.append("]");
        return sb.toString();
    }

    public int hashCode() {
        int h = 1;
        int p = position();
        for (int i = limit() - 1; i >= p; i--)
            h = 31 * h + (int) get(i);
        return h;
    }

    public boolean equals(Object ob) {
        if (this == ob)
            return true;
        if (!(ob instanceof ByteBuffer))
            return false;
        ByteBuffer that = (ByteBuffer) ob;
        // 剩余的个数不相等 limit - position
        if (this.remaining() != that.remaining())
            return false;
        int p = this.position();
        // 只比较剩余的byte是否相等，不是所有的
        for (int i = this.limit() - 1, j = that.limit() - 1; i >= p; i--, j--)
            if (!equals(this.get(i), that.get(j)))
                return false;
        return true;
    }

    private static boolean equals(byte x, byte y) {
        return x == y;
    }

    public int compareTo(ByteBuffer that) {
        int n = this.position() + Math.min(this.remaining(), that.remaining());
        // 比较两个ByteBuffer的剩余元素
        for (int i = this.position(), j = that.position(); i < n; i++, j++) {
            int cmp = compare(this.get(i), that.get(j));
            // 第一个不相等的元素小于另一个中对应的元素，则第一个小于另一个
            if (cmp != 0)
                return cmp;
        }
        // 所有元素都相等，但第一个的元素个数比另一个少，则第一个小于另一个
        return this.remaining() - that.remaining();
    }

    private static int compare(byte x, byte y) {

        return Byte.compare(x, y);

    }

    boolean bigEndian = true;
    boolean nativeByteOrder = (Bits.byteOrder() == ByteOrder.BIG_ENDIAN);

    public final ByteOrder order() {
        return bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
    }

    public final ByteBuffer order(ByteOrder bo) {
        bigEndian = (bo == ByteOrder.BIG_ENDIAN);
        nativeByteOrder = (bigEndian == (Bits.byteOrder() == ByteOrder.BIG_ENDIAN));
        return this;
    }

    abstract byte _get(int i);

    abstract void _put(int i, byte b);

    public abstract char getChar();

    public abstract ByteBuffer putChar(char value);

    public abstract char getChar(int index);

    public abstract ByteBuffer putChar(int index, char value);

    // public abstract CharBuffer asCharBuffer();

    public abstract short getShort();

    public abstract ByteBuffer putShort(short value);

    public abstract short getShort(int index);

    public abstract ByteBuffer putShort(int index, short value);

    // public abstract ShortBuffer asShortBuffer();

    public abstract int getInt();

    public abstract ByteBuffer putInt(int value);

    public abstract int getInt(int index);

    public abstract ByteBuffer putInt(int index, int value);

    // public abstract IntBuffer asIntBuffer();

    public abstract long getLong();

    public abstract ByteBuffer putLong(long value);

    public abstract long getLong(int index);

    public abstract ByteBuffer putLong(int index, long value);

    // public abstract LongBuffer asLongBuffer();

    public abstract float getFloat();

    public abstract ByteBuffer putFloat(float value);

    public abstract float getFloat(int index);

    public abstract ByteBuffer putFloat(int index, float value);

    // public abstract FloatBuffer asFloatBuffer();

    public abstract double getDouble();

    public abstract ByteBuffer putDouble(double value);

    public abstract double getDouble(int index);

    public abstract ByteBuffer putDouble(int index, double value);

    // public abstract DoubleBuffer asDoubleBuffer();

}
