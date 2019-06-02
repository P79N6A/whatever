package mmp.nio.channels;

import mmp.nio.ByteBuffer;

import java.io.IOException;

public interface ScatteringByteChannel extends ReadableByteChannel {

    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException;

    public long read(ByteBuffer[] dsts) throws IOException;

}
