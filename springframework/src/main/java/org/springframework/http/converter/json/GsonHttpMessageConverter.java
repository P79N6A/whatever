package org.springframework.http.converter.json;

import com.google.gson.Gson;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class GsonHttpMessageConverter extends AbstractJsonHttpMessageConverter {

    private Gson gson;

    public GsonHttpMessageConverter() {
        this.gson = new Gson();
    }

    public GsonHttpMessageConverter(Gson gson) {
        Assert.notNull(gson, "A Gson instance is required");
        this.gson = gson;
    }

    public void setGson(Gson gson) {
        Assert.notNull(gson, "A Gson instance is required");
        this.gson = gson;
    }

    public Gson getGson() {
        return this.gson;
    }

    @Override
    protected Object readInternal(Type resolvedType, Reader reader) throws Exception {
        return getGson().fromJson(reader, resolvedType);
    }

    @Override
    protected void writeInternal(Object o, @Nullable Type type, Writer writer) throws Exception {
        // In Gson, toJson with a type argument will exclusively use that given type,
        // ignoring the actual type of the object... which might be more specific,
        // e.g. a subclass of the specified type which includes additional fields.
        // As a consequence, we're only passing in parameterized type declarations
        // which might contain extra generics that the object instance doesn't retain.
        if (type instanceof ParameterizedType) {
            getGson().toJson(o, type, writer);
        } else {
            getGson().toJson(o, writer);
        }
    }

}
