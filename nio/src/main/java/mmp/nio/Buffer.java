package mmp.nio;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.InvalidMarkException;
import java.util.Spliterator;

public abstract class Buffer {

    static final int SPLITERATOR_CHARACTERISTICS = Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.ORDERED;

    // Invariants: mark <= position <= limit <= capacity

    /**
     * 标记，0~position，设置标记会把position移动到mark处
     */
    private int mark = -1;

    /**
     * 指针位置，读写的起始点，初始为0
     * 读取或写入一个单位数据，position++
     * 读写模式共用position和limit，因此需要切换
     */
    private int position = 0;

    /**
     * 读写的终止点，limit之后的区域无法访问
     */
    private int limit;

    /**
     * 缓冲区的最大容量
     */
    private int capacity;

    long address;

    Buffer(int mark, int pos, int lim, int cap) {
        if (cap < 0)
            throw new IllegalArgumentException("Negative capacity: " + cap);
        this.capacity = cap;
        limit(lim);
        position(pos);
        if (mark >= 0) {
            if (mark > pos)
                throw new IllegalArgumentException("mark > position: (" + mark + " > " + pos + ")");
            this.mark = mark;
        }
    }

    /**
     * 返回capacity
     */
    public final int capacity() {
        return capacity;
    }

    /**
     * 返回position
     */
    public final int position() {
        return position;
    }

    /**
     * 设置position
     */
    public final Buffer position(int newPosition) {
        // 检查越界
        if ((newPosition > limit) || (newPosition < 0))
            throw new IllegalArgumentException();
        position = newPosition;
        if (mark > position)
            mark = -1;
        return this;
    }

    /**
     * 返回limit
     */
    public final int limit() {
        return limit;
    }

    /**
     * 设置limit
     */
    public final Buffer limit(int newLimit) {
        // 检查越界
        if ((newLimit > capacity) || (newLimit < 0))
            throw new IllegalArgumentException();
        limit = newLimit;
        if (position > limit)
            position = limit;
        if (mark > limit)
            mark = -1;
        return this;
    }

    /**
     * mark当前position，保存起来
     */
    public final Buffer mark() {
        mark = position;
        return this;
    }

    /**
     * 将position置为mark的位置，一般和mark组合使用
     */
    public final Buffer reset() {
        int m = mark;
        if (m < 0)
            throw new InvalidMarkException();
        position = m;
        return this;
    }

    /**
     * 重置，初始化，但是数据还在，只是恢复位置
     * 切换到写模式：读取了所有的缓冲数据，重置使重新可写
     * 切换到读模式：在已经写满数据的缓冲中，重置使从头读取，类似flip
     */
    public final Buffer clear() {
        position = 0;
        limit = capacity;
        mark = -1;
        return this;
    }

    /**
     * 切换到读模式：确定缓冲区数据的起始点和终止点
     */
    public final Buffer flip() {
        // 原先的写position变成了读limit
        limit = position;
        // position重置为0
        position = 0;
        mark = -1;
        return this;
    }

    /**
     * 倒带，仅将position置为0，mark值无效
     */
    public final Buffer rewind() {
        position = 0;
        mark = -1;
        return this;
    }

    /**
     * 返回position到limit之间元素个数，表示还可以写入/读取多少单位的数据
     */
    public final int remaining() {
        return limit - position;
    }

    /**
     * 判断position到limit之间是否还有元素
     */
    public final boolean hasRemaining() {
        return position < limit;
    }

    public abstract boolean isReadOnly();

    public abstract boolean hasArray();

    public abstract Object array();

    public abstract int arrayOffset();

    public abstract boolean isDirect();

    final int nextGetIndex() {
        if (position >= limit)
            throw new BufferUnderflowException();
        return position++;
    }

    final int nextGetIndex(int nb) {
        if (limit - position < nb)
            throw new BufferUnderflowException();
        int p = position;
        position += nb;
        return p;
    }

    final int nextPutIndex() {
        if (position >= limit)
            throw new BufferOverflowException();
        return position++;
    }

    final int nextPutIndex(int nb) {
        if (limit - position < nb)
            throw new BufferOverflowException();
        int p = position;
        position += nb;
        return p;
    }

    final int checkIndex(int i) {
        if ((i < 0) || (i >= limit))
            throw new IndexOutOfBoundsException();
        return i;
    }

    final int checkIndex(int i, int nb) {
        if ((i < 0) || (nb > limit - i))
            throw new IndexOutOfBoundsException();
        return i;
    }

    final int markValue() {
        return mark;
    }

    /**
     * 截断
     */
    final void truncate() {
        mark = -1;
        position = 0;
        limit = 0;
        capacity = 0;
    }

    final void discardMark() {
        mark = -1;
    }

    /**
     * 起始偏移 长度 数组长度
     */
    static void checkBounds(int off, int len, int size) {
        if ((off | len | (off + len) | (size - (off + len))) < 0)
            throw new IndexOutOfBoundsException();
    }

}
