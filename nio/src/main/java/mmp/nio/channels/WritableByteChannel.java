package mmp.nio.channels;

import mmp.nio.ByteBuffer;

import java.io.IOException;

public interface WritableByteChannel extends Channel {

    public int write(ByteBuffer src) throws IOException;

}
