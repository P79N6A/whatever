package org.apache.dubbo.common.serialize;

import java.io.IOException;

public interface ObjectOutput extends DataOutput {

    void writeObject(Object obj) throws IOException;

}