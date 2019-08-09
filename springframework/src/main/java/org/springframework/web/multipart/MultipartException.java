package org.springframework.web.multipart;

import org.springframework.core.NestedRuntimeException;
import org.springframework.lang.Nullable;

@SuppressWarnings("serial")
public class MultipartException extends NestedRuntimeException {

    public MultipartException(String msg) {
        super(msg);
    }

    public MultipartException(String msg, @Nullable Throwable cause) {
        super(msg, cause);
    }

}
