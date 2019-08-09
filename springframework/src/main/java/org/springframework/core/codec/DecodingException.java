package org.springframework.core.codec;

import org.springframework.lang.Nullable;

@SuppressWarnings("serial")
public class DecodingException extends CodecException {

    public DecodingException(String msg) {
        super(msg);
    }

    public DecodingException(String msg, @Nullable Throwable cause) {
        super(msg, cause);
    }

}
