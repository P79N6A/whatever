package org.springframework.core.codec;

import org.springframework.lang.Nullable;

@SuppressWarnings("serial")
public class EncodingException extends CodecException {

    public EncodingException(String msg) {
        super(msg);
    }

    public EncodingException(String msg, @Nullable Throwable cause) {
        super(msg, cause);
    }

}
