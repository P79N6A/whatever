package org.springframework.boot.json;

public class JsonParseException extends IllegalArgumentException {

    public JsonParseException() {
        this(null);
    }

    public JsonParseException(Throwable cause) {
        super("Cannot parse JSON", cause);
    }

}
