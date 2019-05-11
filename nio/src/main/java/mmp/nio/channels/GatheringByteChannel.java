package mmp.nio.channels;

import mmp.nio.ByteBuffer;

import java.io.IOException;


public interface GatheringByteChannel extends WritableByteChannel {

    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException;


    public long write(ByteBuffer[] srcs) throws IOException;

}
