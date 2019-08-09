package org.springframework.http.converter.json;

import com.google.gson.*;
import org.springframework.util.Base64Utils;

import java.lang.reflect.Type;

public abstract class GsonBuilderUtils {

    public static GsonBuilder gsonBuilderWithBase64EncodedByteArrays() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeHierarchyAdapter(byte[].class, new Base64TypeAdapter());
        return builder;
    }

    private static class Base64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {

        @Override
        public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(Base64Utils.encodeToString(src));
        }

        @Override
        public byte[] deserialize(JsonElement json, Type type, JsonDeserializationContext cxt) {
            return Base64Utils.decodeFromString(json.getAsString());
        }

    }

}
