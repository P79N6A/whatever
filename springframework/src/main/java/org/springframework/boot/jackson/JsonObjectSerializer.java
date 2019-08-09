package org.springframework.boot.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public abstract class JsonObjectSerializer<T> extends JsonSerializer<T> {

    @Override
    public final void serialize(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        try {
            jgen.writeStartObject();
            serializeObject(value, jgen, provider);
            jgen.writeEndObject();
        } catch (Exception ex) {
            if (ex instanceof IOException) {
                throw (IOException) ex;
            }
            throw new JsonMappingException(jgen, "Object serialize error", ex);
        }
    }

    protected abstract void serializeObject(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException;

}
