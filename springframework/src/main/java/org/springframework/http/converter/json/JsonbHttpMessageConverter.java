package org.springframework.http.converter.json;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class JsonbHttpMessageConverter extends AbstractJsonHttpMessageConverter {

    private Jsonb jsonb;

    public JsonbHttpMessageConverter() {
        this(JsonbBuilder.create());
    }

    public JsonbHttpMessageConverter(JsonbConfig config) {
        this.jsonb = JsonbBuilder.create(config);
    }

    public JsonbHttpMessageConverter(Jsonb jsonb) {
        Assert.notNull(jsonb, "A Jsonb instance is required");
        this.jsonb = jsonb;
    }

    public void setJsonb(Jsonb jsonb) {
        Assert.notNull(jsonb, "A Jsonb instance is required");
        this.jsonb = jsonb;
    }

    public Jsonb getJsonb() {
        return this.jsonb;
    }

    @Override
    protected Object readInternal(Type resolvedType, Reader reader) throws Exception {
        return getJsonb().fromJson(reader, resolvedType);
    }

    @Override
    protected void writeInternal(Object o, @Nullable Type type, Writer writer) throws Exception {
        if (type instanceof ParameterizedType) {
            getJsonb().toJson(o, type, writer);
        } else {
            getJsonb().toJson(o, writer);
        }
    }

}
