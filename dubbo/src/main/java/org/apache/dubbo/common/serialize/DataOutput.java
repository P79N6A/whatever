package org.apache.dubbo.common.serialize;

import java.io.IOException;

public interface DataOutput {

    void writeBool(boolean v) throws IOException;

    void writeByte(byte v) throws IOException;

    void writeShort(short v) throws IOException;

    void writeInt(int v) throws IOException;

    void writeLong(long v) throws IOException;

    void writeFloat(float v) throws IOException;

    void writeDouble(double v) throws IOException;

    void writeUTF(String v) throws IOException;

    void writeBytes(byte[] v) throws IOException;

    void writeBytes(byte[] v, int off, int len) throws IOException;

    void flushBuffer() throws IOException;

}