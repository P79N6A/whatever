package io.netty.buffer;

import io.netty.util.ByteProcessor;
import io.netty.util.ReferenceCounted;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;



/*

 +-------------------+------------------+------------------+
| discardable bytes |  readable bytes  |  writable bytes  |
|                   |     (CONTENT)    |                  |
+-------------------+------------------+------------------+
|                   |                  |                  |
0      <=      readerIndex   <=   writerIndex    <=    capacity

ByteBuf由三段构成：废弃段、可读段、可写段
可读段：缓冲区实际存储的可用数据，使用readXXX()或skip()方法时，会增加读索引
废弃段：读索引之前的数据进入废弃段，表示该数据已被使用
可写段：writeXXX()方法向缓冲区写入数据，增加写索引

discardReadBytes()清空废弃段得到跟多的可写空间：

清空后：
+------------------+--------------------------------------+
|  readable bytes  |    writable bytes (got more space)   |
+------------------+--------------------------------------+
|                  |                                      |
readerIndex (0) <= writerIndex (decreased)     <=     capacity

* */


/*

boolean hasArray(); // 判断底层实现是否为字节数组
byte[] array(); // 返回底层实现的字节数组
int arrayOffset(); // 底层字节数组的首字节位置

boolean isDirect(); // 判断底层实现是否为DirectByteBuffer
boolean hasMemoryAddress(); // DirectByteBuffer是否有内存地址
long memoryAddress(); // DirectByteBuffer的首字节内存地址

* */

/*

向缓冲区写入两个字节0x01和0x02，然后读取出这两个字节
如果使用ByteBuffer：

ByteBuffer buf = ByteBuffer.allocate(4);
buf.put((byte) 1);
buf.put((byte) 2);
buf.flip(); // 从写模式切换为读模式
buf.get(); // 取出0x01
buf.get(); // 取出0x02

示例中申请了4个字节的空间，此时理应可以继续写入数据
如果调用buf.put((byte) 3) 将抛出java.nio.BufferOverflowException
需要调用buf.clear()清空整个缓冲区或者buf.compact()清除已经读过的数据

* */

/*

使用ByteBuffer四个步骤：

写入数据到ByteBuffer
flip()
从ByteBuffer中读取数据
clear()或compact()

* */


/*

buf.get(0);
buf.get(1);
buf.get();
buf.get();
get()会增加读索引，get(index)并不会增加读索引
事实上ByteBuffer中并没有读索引和写索引的说法，被统一称为position

* */

/*

如果用Netty的ByteBuf
ByteBuf buf2 = Unpooled.buffer(4);
buf2.writeByte(1);
buf2.writeByte(2);
buf2.readByte();
buf2.readByte();
buf2.writeByte(3);
buf2.writeByte(4);

* */

/*

HeapByteBuf：底层为JAVA堆内的字节数组，由GC回收，申请和释放效率较高，常规JAVA程序使用建议使用
DirectByteBuf：底层实现为操作系统内核空间的字节数组，位于JVM堆外，由操作系统管理，而DirectByteBuf的引用由JVM管理，申请和释放效率都低于堆缓冲区，但可减少拷贝
CompositeByteBuf：组合实现，如果要将后一个缓冲区的数据拷贝到前一个缓冲区，使用组合缓冲区则可以直接保存两个缓冲区而不拷贝
UnpooledByteBuf：不使用对象池的缓冲区，不需要创建大量缓冲区对象时建议使用
PooledByteBuf：对象池缓冲区，当对象释放后会归还给对象池，可循环使用，Netty4.1默认

* */

/*
 * 可以自定义缓冲类型
 * 通过一个内置的复合缓冲类型实现零拷贝
 * 扩展性好，如StringBuffer
 * 不需要调用flip()来切换读/写模式
 * 读取和写入索引分开
 * 方法链
 * 引用计数
 * Pooling(池)
 */

/**
 * 扩展了ReferenceCounted实现引用计数
 */
@SuppressWarnings("ClassMayBeInterface")
public abstract class ByteBuf implements ReferenceCounted, Comparable<ByteBuf> {

    public abstract int capacity();

    public abstract ByteBuf capacity(int newCapacity);

    public abstract int maxCapacity();

    public abstract ByteBufAllocator alloc();

    @Deprecated
    public abstract ByteOrder order();

    @Deprecated
    public abstract ByteBuf order(ByteOrder endianness);

    public abstract ByteBuf unwrap();

    public abstract boolean isDirect();

    public abstract boolean isReadOnly();

    public abstract ByteBuf asReadOnly();

    public abstract int readerIndex();

    public abstract ByteBuf readerIndex(int readerIndex);

    public abstract int writerIndex();

    public abstract ByteBuf writerIndex(int writerIndex);

    public abstract ByteBuf setIndex(int readerIndex, int writerIndex);

    public abstract int readableBytes();

    public abstract int writableBytes();

    public abstract int maxWritableBytes();

    public abstract boolean isReadable();

    public abstract boolean isReadable(int size);

    public abstract boolean isWritable();

    public abstract boolean isWritable(int size);

    /**
     * 清空缓冲区，缓冲区的写索引和读索引都将置0，但是并不清除缓冲区中的实际数据
     */
    public abstract ByteBuf clear();

    /**
     * mark()和reset()标记并重置读索引和写索引
     */
    public abstract ByteBuf markReaderIndex();

    public abstract ByteBuf resetReaderIndex();

    public abstract ByteBuf markWriterIndex();

    public abstract ByteBuf resetWriterIndex();

    /**
     * 可能涉及内存复制，因为需要移动ByteBuf中可读的字节到开始位置
     */
    public abstract ByteBuf discardReadBytes();

    public abstract ByteBuf discardSomeReadBytes();

    public abstract ByteBuf ensureWritable(int minWritableBytes);

    public abstract int ensureWritable(int minWritableBytes, boolean force);

    public abstract boolean getBoolean(int index);

    public abstract byte getByte(int index);

    public abstract short getUnsignedByte(int index);

    public abstract short getShort(int index);

    public abstract short getShortLE(int index);

    public abstract int getUnsignedShort(int index);

    public abstract int getUnsignedShortLE(int index);

    public abstract int getMedium(int index);

    public abstract int getMediumLE(int index);

    public abstract int getUnsignedMedium(int index);

    public abstract int getUnsignedMediumLE(int index);

    public abstract int getInt(int index);

    public abstract int getIntLE(int index);

    public abstract long getUnsignedInt(int index);

    public abstract long getUnsignedIntLE(int index);

    public abstract long getLong(int index);

    public abstract long getLongLE(int index);

    public abstract char getChar(int index);

    public abstract float getFloat(int index);

    public float getFloatLE(int index) {
        return Float.intBitsToFloat(getIntLE(index));
    }

    public abstract double getDouble(int index);

    public double getDoubleLE(int index) {
        return Double.longBitsToDouble(getLongLE(index));
    }

    public abstract ByteBuf getBytes(int index, ByteBuf dst);

    public abstract ByteBuf getBytes(int index, ByteBuf dst, int length);

    public abstract ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length);

    public abstract ByteBuf getBytes(int index, byte[] dst);

    public abstract ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length);

    public abstract ByteBuf getBytes(int index, ByteBuffer dst);

    public abstract ByteBuf getBytes(int index, OutputStream out, int length) throws IOException;

    public abstract int getBytes(int index, GatheringByteChannel out, int length) throws IOException;

    public abstract int getBytes(int index, FileChannel out, long position, int length) throws IOException;

    public abstract CharSequence getCharSequence(int index, int length, Charset charset);

    public abstract ByteBuf setBoolean(int index, boolean value);

    public abstract ByteBuf setByte(int index, int value);

    public abstract ByteBuf setShort(int index, int value);

    public abstract ByteBuf setShortLE(int index, int value);

    public abstract ByteBuf setMedium(int index, int value);

    public abstract ByteBuf setMediumLE(int index, int value);

    public abstract ByteBuf setInt(int index, int value);

    public abstract ByteBuf setIntLE(int index, int value);

    public abstract ByteBuf setLong(int index, long value);

    public abstract ByteBuf setLongLE(int index, long value);

    public abstract ByteBuf setChar(int index, int value);

    public abstract ByteBuf setFloat(int index, float value);

    public ByteBuf setFloatLE(int index, float value) {
        return setIntLE(index, Float.floatToRawIntBits(value));
    }

    public abstract ByteBuf setDouble(int index, double value);

    public ByteBuf setDoubleLE(int index, double value) {
        return setLongLE(index, Double.doubleToRawLongBits(value));
    }

    public abstract ByteBuf setBytes(int index, ByteBuf src);

    public abstract ByteBuf setBytes(int index, ByteBuf src, int length);

    public abstract ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length);

    public abstract ByteBuf setBytes(int index, byte[] src);

    public abstract ByteBuf setBytes(int index, byte[] src, int srcIndex, int length);

    public abstract ByteBuf setBytes(int index, ByteBuffer src);

    public abstract int setBytes(int index, InputStream in, int length) throws IOException;

    public abstract int setBytes(int index, ScatteringByteChannel in, int length) throws IOException;

    public abstract int setBytes(int index, FileChannel in, long position, int length) throws IOException;

    public abstract ByteBuf setZero(int index, int length);

    public abstract int setCharSequence(int index, CharSequence sequence, Charset charset);

    public abstract boolean readBoolean();

    public abstract byte readByte();

    public abstract short readUnsignedByte();

    public abstract short readShort();

    public abstract short readShortLE();

    public abstract int readUnsignedShort();

    public abstract int readUnsignedShortLE();

    public abstract int readMedium();

    public abstract int readMediumLE();

    public abstract int readUnsignedMedium();

    public abstract int readUnsignedMediumLE();

    public abstract int readInt();

    public abstract int readIntLE();

    public abstract long readUnsignedInt();

    public abstract long readUnsignedIntLE();

    public abstract long readLong();

    public abstract long readLongLE();

    public abstract char readChar();

    public abstract float readFloat();

    public float readFloatLE() {
        return Float.intBitsToFloat(readIntLE());
    }

    public abstract double readDouble();

    public double readDoubleLE() {
        return Double.longBitsToDouble(readLongLE());
    }

    public abstract ByteBuf readBytes(int length);

    public abstract ByteBuf readSlice(int length);

    public abstract ByteBuf readRetainedSlice(int length);

    public abstract ByteBuf readBytes(ByteBuf dst);

    public abstract ByteBuf readBytes(ByteBuf dst, int length);

    public abstract ByteBuf readBytes(ByteBuf dst, int dstIndex, int length);

    public abstract ByteBuf readBytes(byte[] dst);

    public abstract ByteBuf readBytes(byte[] dst, int dstIndex, int length);

    public abstract ByteBuf readBytes(ByteBuffer dst);

    public abstract ByteBuf readBytes(OutputStream out, int length) throws IOException;

    public abstract int readBytes(GatheringByteChannel out, int length) throws IOException;

    public abstract CharSequence readCharSequence(int length, Charset charset);

    public abstract int readBytes(FileChannel out, long position, int length) throws IOException;

    public abstract ByteBuf skipBytes(int length);

    public abstract ByteBuf writeBoolean(boolean value);

    public abstract ByteBuf writeByte(int value);

    public abstract ByteBuf writeShort(int value);

    public abstract ByteBuf writeShortLE(int value);

    public abstract ByteBuf writeMedium(int value);

    public abstract ByteBuf writeMediumLE(int value);

    public abstract ByteBuf writeInt(int value);

    public abstract ByteBuf writeIntLE(int value);

    public abstract ByteBuf writeLong(long value);

    public abstract ByteBuf writeLongLE(long value);

    public abstract ByteBuf writeChar(int value);

    public abstract ByteBuf writeFloat(float value);

    public ByteBuf writeFloatLE(float value) {
        return writeIntLE(Float.floatToRawIntBits(value));
    }

    public abstract ByteBuf writeDouble(double value);

    public ByteBuf writeDoubleLE(double value) {
        return writeLongLE(Double.doubleToRawLongBits(value));
    }

    public abstract ByteBuf writeBytes(ByteBuf src);

    public abstract ByteBuf writeBytes(ByteBuf src, int length);

    public abstract ByteBuf writeBytes(ByteBuf src, int srcIndex, int length);

    public abstract ByteBuf writeBytes(byte[] src);

    public abstract ByteBuf writeBytes(byte[] src, int srcIndex, int length);

    public abstract ByteBuf writeBytes(ByteBuffer src);

    public abstract int writeBytes(InputStream in, int length) throws IOException;

    public abstract int writeBytes(ScatteringByteChannel in, int length) throws IOException;

    public abstract int writeBytes(FileChannel in, long position, int length) throws IOException;

    public abstract ByteBuf writeZero(int length);

    public abstract int writeCharSequence(CharSequence sequence, Charset charset);

    public abstract int indexOf(int fromIndex, int toIndex, byte value);

    public abstract int bytesBefore(byte value);

    public abstract int bytesBefore(int length, byte value);

    public abstract int bytesBefore(int index, int length, byte value);

    public abstract int forEachByte(ByteProcessor processor);

    public abstract int forEachByte(int index, int length, ByteProcessor processor);

    public abstract int forEachByteDesc(ByteProcessor processor);

    public abstract int forEachByteDesc(int index, int length, ByteProcessor processor);

    public abstract ByteBuf copy();

    public abstract ByteBuf copy(int index, int length);

    public abstract ByteBuf slice();

    public abstract ByteBuf retainedSlice();

    public abstract ByteBuf slice(int index, int length);

    public abstract ByteBuf retainedSlice(int index, int length);

    public abstract ByteBuf duplicate();

    public abstract ByteBuf retainedDuplicate();

    public abstract int nioBufferCount();

    public abstract ByteBuffer nioBuffer();

    public abstract ByteBuffer nioBuffer(int index, int length);

    public abstract ByteBuffer internalNioBuffer(int index, int length);

    public abstract ByteBuffer[] nioBuffers();

    public abstract ByteBuffer[] nioBuffers(int index, int length);

    public abstract boolean hasArray();

    public abstract byte[] array();

    public abstract int arrayOffset();

    public abstract boolean hasMemoryAddress();

    public abstract long memoryAddress();

    public abstract String toString(Charset charset);

    public abstract String toString(int index, int length, Charset charset);

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int compareTo(ByteBuf buffer);

    @Override
    public abstract String toString();

    @Override
    public abstract ByteBuf retain(int increment);

    @Override
    public abstract ByteBuf retain();

    @Override
    public abstract ByteBuf touch();

    @Override
    public abstract ByteBuf touch(Object hint);

    boolean isAccessible() {
        return refCnt() != 0;
    }
}
