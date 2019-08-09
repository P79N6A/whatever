package org.springframework.boot.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.springframework.util.Assert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

public abstract class JsonObjectDeserializer<T> extends com.fasterxml.jackson.databind.JsonDeserializer<T> {

    @Override
    public final T deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        try {
            ObjectCodec codec = jp.getCodec();
            JsonNode tree = codec.readTree(jp);
            return deserializeObject(jp, ctxt, codec, tree);
        } catch (Exception ex) {
            if (ex instanceof IOException) {
                throw (IOException) ex;
            }
            throw new JsonMappingException(jp, "Object deserialize error", ex);
        }
    }

    protected abstract T deserializeObject(JsonParser jsonParser, DeserializationContext context, ObjectCodec codec, JsonNode tree) throws IOException;

    @SuppressWarnings({"unchecked"})
    protected final <D> D nullSafeValue(JsonNode jsonNode, Class<D> type) {
        Assert.notNull(type, "Type must not be null");
        if (jsonNode == null) {
            return null;
        }
        if (type == String.class) {
            return (D) jsonNode.textValue();
        }
        if (type == Boolean.class) {
            return (D) Boolean.valueOf(jsonNode.booleanValue());
        }
        if (type == Long.class) {
            return (D) Long.valueOf(jsonNode.longValue());
        }
        if (type == Integer.class) {
            return (D) Integer.valueOf(jsonNode.intValue());
        }
        if (type == Short.class) {
            return (D) Short.valueOf(jsonNode.shortValue());
        }
        if (type == Double.class) {
            return (D) Double.valueOf(jsonNode.doubleValue());
        }
        if (type == Float.class) {
            return (D) Float.valueOf(jsonNode.floatValue());
        }
        if (type == BigDecimal.class) {
            return (D) jsonNode.decimalValue();
        }
        if (type == BigInteger.class) {
            return (D) jsonNode.bigIntegerValue();
        }
        throw new IllegalArgumentException("Unsupported value type " + type.getName());
    }

    protected final JsonNode getRequiredNode(JsonNode tree, String fieldName) {
        Assert.notNull(tree, "Tree must not be null");
        JsonNode node = tree.get(fieldName);
        Assert.state(node != null && !(node instanceof NullNode), () -> "Missing JSON field '" + fieldName + "'");
        return node;
    }

}
