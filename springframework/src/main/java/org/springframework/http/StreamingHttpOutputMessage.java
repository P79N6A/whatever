package org.springframework.http;

import java.io.IOException;
import java.io.OutputStream;

public interface StreamingHttpOutputMessage extends HttpOutputMessage {

    void setBody(Body body);

    @FunctionalInterface
    interface Body {

        void writeTo(OutputStream outputStream) throws IOException;

    }

}
