package org.springframework.http.codec.protobuf;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class ProtobufDecoder extends ProtobufCodecSupport implements Decoder<Message> {

    protected static final int DEFAULT_MESSAGE_MAX_SIZE = 64 * 1024;

    private static final ConcurrentMap<Class<?>, Method> methodCache = new ConcurrentReferenceHashMap<>();

    private final ExtensionRegistry extensionRegistry;

    private int maxMessageSize = DEFAULT_MESSAGE_MAX_SIZE;

    public ProtobufDecoder() {
        this(ExtensionRegistry.newInstance());
    }

    public ProtobufDecoder(ExtensionRegistry extensionRegistry) {
        Assert.notNull(extensionRegistry, "ExtensionRegistry must not be null");
        this.extensionRegistry = extensionRegistry;
    }

    public void setMaxMessageSize(int maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
    }

    @Override
    public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
        return Message.class.isAssignableFrom(elementType.toClass()) && supportsMimeType(mimeType);
    }

    @Override
    public Flux<Message> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
        MessageDecoderFunction decoderFunction = new MessageDecoderFunction(elementType, this.maxMessageSize);
        return Flux.from(inputStream).flatMapIterable(decoderFunction).doOnTerminate(decoderFunction::discard);
    }

    @Override
    public Mono<Message> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
        return DataBufferUtils.join(inputStream).map(dataBuffer -> decode(dataBuffer, elementType, mimeType, hints));
    }

    @Override
    public Message decode(DataBuffer dataBuffer, ResolvableType targetType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) throws DecodingException {
        try {
            Message.Builder builder = getMessageBuilder(targetType.toClass());
            ByteBuffer buffer = dataBuffer.asByteBuffer();
            builder.mergeFrom(CodedInputStream.newInstance(buffer), this.extensionRegistry);
            return builder.build();
        } catch (IOException ex) {
            throw new DecodingException("I/O error while parsing input stream", ex);
        } catch (Exception ex) {
            throw new DecodingException("Could not read Protobuf message: " + ex.getMessage(), ex);
        } finally {
            DataBufferUtils.release(dataBuffer);
        }
    }

    private static Message.Builder getMessageBuilder(Class<?> clazz) throws Exception {
        Method method = methodCache.get(clazz);
        if (method == null) {
            method = clazz.getMethod("newBuilder");
            methodCache.put(clazz, method);
        }
        return (Message.Builder) method.invoke(clazz);
    }

    @Override
    public List<MimeType> getDecodableMimeTypes() {
        return getMimeTypes();
    }

    private class MessageDecoderFunction implements Function<DataBuffer, Iterable<? extends Message>> {

        private final ResolvableType elementType;

        private final int maxMessageSize;

        @Nullable
        private DataBuffer output;

        private int messageBytesToRead;

        private int offset;

        public MessageDecoderFunction(ResolvableType elementType, int maxMessageSize) {
            this.elementType = elementType;
            this.maxMessageSize = maxMessageSize;
        }

        @Override
        public Iterable<? extends Message> apply(DataBuffer input) {
            try {
                List<Message> messages = new ArrayList<>();
                int remainingBytesToRead;
                int chunkBytesToRead;
                do {
                    if (this.output == null) {
                        if (!readMessageSize(input)) {
                            return messages;
                        }
                        if (this.messageBytesToRead > this.maxMessageSize) {
                            throw new DecodingException("The number of bytes to read from the incoming stream " + "(" + this.messageBytesToRead + ") exceeds " + "the configured limit (" + this.maxMessageSize + ")");
                        }
                        this.output = input.factory().allocateBuffer(this.messageBytesToRead);
                    }
                    chunkBytesToRead = this.messageBytesToRead >= input.readableByteCount() ? input.readableByteCount() : this.messageBytesToRead;
                    remainingBytesToRead = input.readableByteCount() - chunkBytesToRead;
                    byte[] bytesToWrite = new byte[chunkBytesToRead];
                    input.read(bytesToWrite, 0, chunkBytesToRead);
                    this.output.write(bytesToWrite);
                    this.messageBytesToRead -= chunkBytesToRead;
                    if (this.messageBytesToRead == 0) {
                        CodedInputStream stream = CodedInputStream.newInstance(this.output.asByteBuffer());
                        DataBufferUtils.release(this.output);
                        this.output = null;
                        Message message = getMessageBuilder(this.elementType.toClass()).mergeFrom(stream, extensionRegistry).build();
                        messages.add(message);
                    }
                } while (remainingBytesToRead > 0);
                return messages;
            } catch (DecodingException ex) {
                throw ex;
            } catch (IOException ex) {
                throw new DecodingException("I/O error while parsing input stream", ex);
            } catch (Exception ex) {
                throw new DecodingException("Could not read Protobuf message: " + ex.getMessage(), ex);
            } finally {
                DataBufferUtils.release(input);
            }
        }

        private boolean readMessageSize(DataBuffer input) {
            if (this.offset == 0) {
                if (input.readableByteCount() == 0) {
                    return false;
                }
                int firstByte = input.read();
                if ((firstByte & 0x80) == 0) {
                    this.messageBytesToRead = firstByte;
                    return true;
                }
                this.messageBytesToRead = firstByte & 0x7f;
                this.offset = 7;
            }
            if (this.offset < 32) {
                for (; this.offset < 32; this.offset += 7) {
                    if (input.readableByteCount() == 0) {
                        return false;
                    }
                    final int b = input.read();
                    this.messageBytesToRead |= (b & 0x7f) << offset;
                    if ((b & 0x80) == 0) {
                        this.offset = 0;
                        return true;
                    }
                }
            }
            // Keep reading up to 64 bits.
            for (; this.offset < 64; this.offset += 7) {
                if (input.readableByteCount() == 0) {
                    return false;
                }
                final int b = input.read();
                if ((b & 0x80) == 0) {
                    this.offset = 0;
                    return true;
                }
            }
            this.offset = 0;
            throw new DecodingException("Cannot parse message size: malformed varint");
        }

        public void discard() {
            if (this.output != null) {
                DataBufferUtils.release(this.output);
            }
        }

    }

}
