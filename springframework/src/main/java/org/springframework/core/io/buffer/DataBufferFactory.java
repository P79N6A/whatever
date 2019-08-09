package org.springframework.core.io.buffer;

import java.nio.ByteBuffer;
import java.util.List;

public interface DataBufferFactory {

    DataBuffer allocateBuffer();

    DataBuffer allocateBuffer(int initialCapacity);

    DataBuffer wrap(ByteBuffer byteBuffer);

    DataBuffer wrap(byte[] bytes);

    DataBuffer join(List<? extends DataBuffer> dataBuffers);

}
