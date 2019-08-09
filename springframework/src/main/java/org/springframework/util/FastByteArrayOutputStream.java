package org.springframework.util;

import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.LinkedList;

public class FastByteArrayOutputStream extends OutputStream {

    private static final int DEFAULT_BLOCK_SIZE = 256;

    // The buffers used to store the content bytes
    private final LinkedList<byte[]> buffers = new LinkedList<>();

    // The size, in bytes, to use when allocating the first byte[]
    private final int initialBlockSize;

    // The size, in bytes, to use when allocating the next byte[]
    private int nextBlockSize = 0;

    // The number of bytes in previous buffers.
    // (The number of bytes in the current buffer is in 'index'.)
    private int alreadyBufferedSize = 0;

    // The index in the byte[] found at buffers.getLast() to be written next
    private int index = 0;

    // Is the stream closed?
    private boolean closed = false;

    public FastByteArrayOutputStream() {
        this(DEFAULT_BLOCK_SIZE);
    }

    public FastByteArrayOutputStream(int initialBlockSize) {
        Assert.isTrue(initialBlockSize > 0, "Initial block size must be greater than 0");
        this.initialBlockSize = initialBlockSize;
        this.nextBlockSize = initialBlockSize;
    }
    // Overridden methods

    @Override
    public void write(int datum) throws IOException {
        if (this.closed) {
            throw new IOException("Stream closed");
        } else {
            if (this.buffers.peekLast() == null || this.buffers.getLast().length == this.index) {
                addBuffer(1);
            }
            // store the byte
            this.buffers.getLast()[this.index++] = (byte) datum;
        }
    }

    @Override
    public void write(byte[] data, int offset, int length) throws IOException {
        if (offset < 0 || offset + length > data.length || length < 0) {
            throw new IndexOutOfBoundsException();
        } else if (this.closed) {
            throw new IOException("Stream closed");
        } else {
            if (this.buffers.peekLast() == null || this.buffers.getLast().length == this.index) {
                addBuffer(length);
            }
            if (this.index + length > this.buffers.getLast().length) {
                int pos = offset;
                do {
                    if (this.index == this.buffers.getLast().length) {
                        addBuffer(length);
                    }
                    int copyLength = this.buffers.getLast().length - this.index;
                    if (length < copyLength) {
                        copyLength = length;
                    }
                    System.arraycopy(data, pos, this.buffers.getLast(), this.index, copyLength);
                    pos += copyLength;
                    this.index += copyLength;
                    length -= copyLength;
                } while (length > 0);
            } else {
                // copy in the sub-array
                System.arraycopy(data, offset, this.buffers.getLast(), this.index, length);
                this.index += length;
            }
        }
    }

    @Override
    public void close() {
        this.closed = true;
    }

    @Override
    public String toString() {
        return new String(toByteArrayUnsafe());
    }
    // Custom methods

    public int size() {
        return (this.alreadyBufferedSize + this.index);
    }

    public byte[] toByteArrayUnsafe() {
        int totalSize = size();
        if (totalSize == 0) {
            return new byte[0];
        }
        resize(totalSize);
        return this.buffers.getFirst();
    }

    public byte[] toByteArray() {
        byte[] bytesUnsafe = toByteArrayUnsafe();
        byte[] ret = new byte[bytesUnsafe.length];
        System.arraycopy(bytesUnsafe, 0, ret, 0, bytesUnsafe.length);
        return ret;
    }

    public void reset() {
        this.buffers.clear();
        this.nextBlockSize = this.initialBlockSize;
        this.closed = false;
        this.index = 0;
        this.alreadyBufferedSize = 0;
    }

    public InputStream getInputStream() {
        return new FastByteArrayInputStream(this);
    }

    public void writeTo(OutputStream out) throws IOException {
        Iterator<byte[]> it = this.buffers.iterator();
        while (it.hasNext()) {
            byte[] bytes = it.next();
            if (it.hasNext()) {
                out.write(bytes, 0, bytes.length);
            } else {
                out.write(bytes, 0, this.index);
            }
        }
    }

    public void resize(int targetCapacity) {
        Assert.isTrue(targetCapacity >= size(), "New capacity must not be smaller than current size");
        if (this.buffers.peekFirst() == null) {
            this.nextBlockSize = targetCapacity - size();
        } else if (size() == targetCapacity && this.buffers.getFirst().length == targetCapacity) {
            // do nothing - already at the targetCapacity
        } else {
            int totalSize = size();
            byte[] data = new byte[targetCapacity];
            int pos = 0;
            Iterator<byte[]> it = this.buffers.iterator();
            while (it.hasNext()) {
                byte[] bytes = it.next();
                if (it.hasNext()) {
                    System.arraycopy(bytes, 0, data, pos, bytes.length);
                    pos += bytes.length;
                } else {
                    System.arraycopy(bytes, 0, data, pos, this.index);
                }
            }
            this.buffers.clear();
            this.buffers.add(data);
            this.index = totalSize;
            this.alreadyBufferedSize = 0;
        }
    }

    private void addBuffer(int minCapacity) {
        if (this.buffers.peekLast() != null) {
            this.alreadyBufferedSize += this.index;
            this.index = 0;
        }
        if (this.nextBlockSize < minCapacity) {
            this.nextBlockSize = nextPowerOf2(minCapacity);
        }
        this.buffers.add(new byte[this.nextBlockSize]);
        this.nextBlockSize *= 2;  // block size doubles each time
    }

    private static int nextPowerOf2(int val) {
        val--;
        val = (val >> 1) | val;
        val = (val >> 2) | val;
        val = (val >> 4) | val;
        val = (val >> 8) | val;
        val = (val >> 16) | val;
        val++;
        return val;
    }

    private static final class FastByteArrayInputStream extends UpdateMessageDigestInputStream {

        private final FastByteArrayOutputStream fastByteArrayOutputStream;

        private final Iterator<byte[]> buffersIterator;

        @Nullable
        private byte[] currentBuffer;

        private int currentBufferLength = 0;

        private int nextIndexInCurrentBuffer = 0;

        private int totalBytesRead = 0;

        public FastByteArrayInputStream(FastByteArrayOutputStream fastByteArrayOutputStream) {
            this.fastByteArrayOutputStream = fastByteArrayOutputStream;
            this.buffersIterator = fastByteArrayOutputStream.buffers.iterator();
            if (this.buffersIterator.hasNext()) {
                this.currentBuffer = this.buffersIterator.next();
                if (this.currentBuffer == fastByteArrayOutputStream.buffers.getLast()) {
                    this.currentBufferLength = fastByteArrayOutputStream.index;
                } else {
                    this.currentBufferLength = (this.currentBuffer != null ? this.currentBuffer.length : 0);
                }
            }
        }

        @Override
        public int read() {
            if (this.currentBuffer == null) {
                // This stream doesn't have any data in it...
                return -1;
            } else {
                if (this.nextIndexInCurrentBuffer < this.currentBufferLength) {
                    this.totalBytesRead++;
                    return this.currentBuffer[this.nextIndexInCurrentBuffer++] & 0xFF;
                } else {
                    if (this.buffersIterator.hasNext()) {
                        this.currentBuffer = this.buffersIterator.next();
                        updateCurrentBufferLength();
                        this.nextIndexInCurrentBuffer = 0;
                    } else {
                        this.currentBuffer = null;
                    }
                    return read();
                }
            }
        }

        @Override
        public int read(byte[] b) {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            } else {
                if (this.currentBuffer == null) {
                    // This stream doesn't have any data in it...
                    return -1;
                } else {
                    if (this.nextIndexInCurrentBuffer < this.currentBufferLength) {
                        int bytesToCopy = Math.min(len, this.currentBufferLength - this.nextIndexInCurrentBuffer);
                        System.arraycopy(this.currentBuffer, this.nextIndexInCurrentBuffer, b, off, bytesToCopy);
                        this.totalBytesRead += bytesToCopy;
                        this.nextIndexInCurrentBuffer += bytesToCopy;
                        int remaining = read(b, off + bytesToCopy, len - bytesToCopy);
                        return bytesToCopy + Math.max(remaining, 0);
                    } else {
                        if (this.buffersIterator.hasNext()) {
                            this.currentBuffer = this.buffersIterator.next();
                            updateCurrentBufferLength();
                            this.nextIndexInCurrentBuffer = 0;
                        } else {
                            this.currentBuffer = null;
                        }
                        return read(b, off, len);
                    }
                }
            }
        }

        @Override
        public long skip(long n) throws IOException {
            if (n > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("n exceeds maximum (" + Integer.MAX_VALUE + "): " + n);
            } else if (n == 0) {
                return 0;
            } else if (n < 0) {
                throw new IllegalArgumentException("n must be 0 or greater: " + n);
            }
            int len = (int) n;
            if (this.currentBuffer == null) {
                // This stream doesn't have any data in it...
                return 0;
            } else {
                if (this.nextIndexInCurrentBuffer < this.currentBufferLength) {
                    int bytesToSkip = Math.min(len, this.currentBufferLength - this.nextIndexInCurrentBuffer);
                    this.totalBytesRead += bytesToSkip;
                    this.nextIndexInCurrentBuffer += bytesToSkip;
                    return (bytesToSkip + skip(len - bytesToSkip));
                } else {
                    if (this.buffersIterator.hasNext()) {
                        this.currentBuffer = this.buffersIterator.next();
                        updateCurrentBufferLength();
                        this.nextIndexInCurrentBuffer = 0;
                    } else {
                        this.currentBuffer = null;
                    }
                    return skip(len);
                }
            }
        }

        @Override
        public int available() {
            return (this.fastByteArrayOutputStream.size() - this.totalBytesRead);
        }

        @Override
        public void updateMessageDigest(MessageDigest messageDigest) {
            updateMessageDigest(messageDigest, available());
        }

        @Override
        public void updateMessageDigest(MessageDigest messageDigest, int len) {
            if (this.currentBuffer == null) {
                // This stream doesn't have any data in it...
                return;
            } else if (len == 0) {
                return;
            } else if (len < 0) {
                throw new IllegalArgumentException("len must be 0 or greater: " + len);
            } else {
                if (this.nextIndexInCurrentBuffer < this.currentBufferLength) {
                    int bytesToCopy = Math.min(len, this.currentBufferLength - this.nextIndexInCurrentBuffer);
                    messageDigest.update(this.currentBuffer, this.nextIndexInCurrentBuffer, bytesToCopy);
                    this.nextIndexInCurrentBuffer += bytesToCopy;
                    updateMessageDigest(messageDigest, len - bytesToCopy);
                } else {
                    if (this.buffersIterator.hasNext()) {
                        this.currentBuffer = this.buffersIterator.next();
                        updateCurrentBufferLength();
                        this.nextIndexInCurrentBuffer = 0;
                    } else {
                        this.currentBuffer = null;
                    }
                    updateMessageDigest(messageDigest, len);
                }
            }
        }

        private void updateCurrentBufferLength() {
            if (this.currentBuffer == this.fastByteArrayOutputStream.buffers.getLast()) {
                this.currentBufferLength = this.fastByteArrayOutputStream.index;
            } else {
                this.currentBufferLength = (this.currentBuffer != null ? this.currentBuffer.length : 0);
            }
        }

    }

}
