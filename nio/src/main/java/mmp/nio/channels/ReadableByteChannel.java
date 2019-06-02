package mmp.nio.channels;

import mmp.nio.ByteBuffer;

import java.io.IOException;

public interface ReadableByteChannel extends Channel {

    public int read(ByteBuffer dst) throws IOException;

}
