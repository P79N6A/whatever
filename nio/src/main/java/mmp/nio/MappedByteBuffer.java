package mmp.nio;

import java.io.FileDescriptor;

public abstract class MappedByteBuffer extends ByteBuffer {

    private final FileDescriptor fd;

    MappedByteBuffer(int mark, int pos, int lim, int cap, FileDescriptor fd) {
        super(mark, pos, lim, cap);
        this.fd = fd;
    }

    MappedByteBuffer(int mark, int pos, int lim, int cap) {
        super(mark, pos, lim, cap);
        this.fd = null;
    }

}
