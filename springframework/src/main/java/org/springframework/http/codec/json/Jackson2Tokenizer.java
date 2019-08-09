package org.springframework.http.codec.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class Jackson2Tokenizer {

    private final JsonParser parser;

    private final DeserializationContext deserializationContext;

    private final boolean tokenizeArrayElements;

    private TokenBuffer tokenBuffer;

    private int objectDepth;

    private int arrayDepth;

    // TODO: change to ByteBufferFeeder when supported by Jackson
    // See https://github.com/FasterXML/jackson-core/issues/478
    private final ByteArrayFeeder inputFeeder;

    private Jackson2Tokenizer(JsonParser parser, DeserializationContext deserializationContext, boolean tokenizeArrayElements) {
        this.parser = parser;
        this.deserializationContext = deserializationContext;
        this.tokenizeArrayElements = tokenizeArrayElements;
        this.tokenBuffer = new TokenBuffer(parser, deserializationContext);
        this.inputFeeder = (ByteArrayFeeder) this.parser.getNonBlockingInputFeeder();
    }

    private List<TokenBuffer> tokenize(DataBuffer dataBuffer) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        DataBufferUtils.release(dataBuffer);
        try {
            this.inputFeeder.feedInput(bytes, 0, bytes.length);
            return parseTokenBufferFlux();
        } catch (JsonProcessingException ex) {
            throw new DecodingException("JSON decoding error: " + ex.getOriginalMessage(), ex);
        } catch (IOException ex) {
            throw Exceptions.propagate(ex);
        }
    }

    private Flux<TokenBuffer> endOfInput() {
        return Flux.defer(() -> {
            this.inputFeeder.endOfInput();
            try {
                return Flux.fromIterable(parseTokenBufferFlux());
            } catch (JsonProcessingException ex) {
                throw new DecodingException("JSON decoding error: " + ex.getOriginalMessage(), ex);
            } catch (IOException ex) {
                throw Exceptions.propagate(ex);
            }
        });
    }

    private List<TokenBuffer> parseTokenBufferFlux() throws IOException {
        List<TokenBuffer> result = new ArrayList<>();
        while (true) {
            JsonToken token = this.parser.nextToken();
            // SPR-16151: Smile data format uses null to separate documents
            if (token == JsonToken.NOT_AVAILABLE || (token == null && (token = this.parser.nextToken()) == null)) {
                break;
            }
            updateDepth(token);
            if (!this.tokenizeArrayElements) {
                processTokenNormal(token, result);
            } else {
                processTokenArray(token, result);
            }
        }
        return result;
    }

    private void updateDepth(JsonToken token) {
        switch (token) {
            case START_OBJECT:
                this.objectDepth++;
                break;
            case END_OBJECT:
                this.objectDepth--;
                break;
            case START_ARRAY:
                this.arrayDepth++;
                break;
            case END_ARRAY:
                this.arrayDepth--;
                break;
        }
    }

    private void processTokenNormal(JsonToken token, List<TokenBuffer> result) throws IOException {
        this.tokenBuffer.copyCurrentEvent(this.parser);
        if ((token.isStructEnd() || token.isScalarValue()) && this.objectDepth == 0 && this.arrayDepth == 0) {
            result.add(this.tokenBuffer);
            this.tokenBuffer = new TokenBuffer(this.parser, this.deserializationContext);
        }

    }

    private void processTokenArray(JsonToken token, List<TokenBuffer> result) throws IOException {
        if (!isTopLevelArrayToken(token)) {
            this.tokenBuffer.copyCurrentEvent(this.parser);
        }
        if (this.objectDepth == 0 && (this.arrayDepth == 0 || this.arrayDepth == 1) && (token == JsonToken.END_OBJECT || token.isScalarValue())) {
            result.add(this.tokenBuffer);
            this.tokenBuffer = new TokenBuffer(this.parser, this.deserializationContext);
        }
    }

    private boolean isTopLevelArrayToken(JsonToken token) {
        return this.objectDepth == 0 && ((token == JsonToken.START_ARRAY && this.arrayDepth == 1) || (token == JsonToken.END_ARRAY && this.arrayDepth == 0));
    }

    public static Flux<TokenBuffer> tokenize(Flux<DataBuffer> dataBuffers, JsonFactory jsonFactory, ObjectMapper objectMapper, boolean tokenizeArrayElements) {
        try {
            JsonParser parser = jsonFactory.createNonBlockingByteArrayParser();
            DeserializationContext context = objectMapper.getDeserializationContext();
            if (context instanceof DefaultDeserializationContext) {
                context = ((DefaultDeserializationContext) context).createInstance(objectMapper.getDeserializationConfig(), parser, objectMapper.getInjectableValues());
            }
            Jackson2Tokenizer tokenizer = new Jackson2Tokenizer(parser, context, tokenizeArrayElements);
            return dataBuffers.concatMapIterable(tokenizer::tokenize).concatWith(tokenizer.endOfInput());
        } catch (IOException ex) {
            return Flux.error(ex);
        }
    }

}
