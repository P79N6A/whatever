package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class SseEmitter extends ResponseBodyEmitter {

    static final MediaType TEXT_PLAIN = new MediaType("text", "plain", StandardCharsets.UTF_8);

    static final MediaType UTF8_TEXT_EVENTSTREAM = new MediaType("text", "event-stream", StandardCharsets.UTF_8);

    public SseEmitter() {
        super();
    }

    public SseEmitter(Long timeout) {
        super(timeout);
    }

    @Override
    protected void extendResponse(ServerHttpResponse outputMessage) {
        super.extendResponse(outputMessage);
        HttpHeaders headers = outputMessage.getHeaders();
        if (headers.getContentType() == null) {
            headers.setContentType(UTF8_TEXT_EVENTSTREAM);
        }
    }

    @Override
    public void send(Object object) throws IOException {
        send(object, null);
    }

    @Override
    public void send(Object object, @Nullable MediaType mediaType) throws IOException {
        send(event().data(object, mediaType));
    }

    public void send(SseEventBuilder builder) throws IOException {
        Set<DataWithMediaType> dataToSend = builder.build();
        synchronized (this) {
            for (DataWithMediaType entry : dataToSend) {
                super.send(entry.getData(), entry.getMediaType());
            }
        }
    }

    @Override
    public String toString() {
        return "SseEmitter@" + ObjectUtils.getIdentityHexString(this);
    }

    public static SseEventBuilder event() {
        return new SseEventBuilderImpl();
    }

    public interface SseEventBuilder {

        SseEventBuilder id(String id);

        SseEventBuilder name(String eventName);

        SseEventBuilder reconnectTime(long reconnectTimeMillis);

        SseEventBuilder comment(String comment);

        SseEventBuilder data(Object object);

        SseEventBuilder data(Object object, @Nullable MediaType mediaType);

        Set<DataWithMediaType> build();

    }

    private static class SseEventBuilderImpl implements SseEventBuilder {

        private final Set<DataWithMediaType> dataToSend = new LinkedHashSet<>(4);

        @Nullable
        private StringBuilder sb;

        @Override
        public SseEventBuilder id(String id) {
            append("id:").append(id).append("\n");
            return this;
        }

        @Override
        public SseEventBuilder name(String name) {
            append("event:").append(name).append("\n");
            return this;
        }

        @Override
        public SseEventBuilder reconnectTime(long reconnectTimeMillis) {
            append("retry:").append(String.valueOf(reconnectTimeMillis)).append("\n");
            return this;
        }

        @Override
        public SseEventBuilder comment(String comment) {
            append(":").append(comment).append("\n");
            return this;
        }

        @Override
        public SseEventBuilder data(Object object) {
            return data(object, null);
        }

        @Override
        public SseEventBuilder data(Object object, @Nullable MediaType mediaType) {
            append("data:");
            saveAppendedText();
            this.dataToSend.add(new DataWithMediaType(object, mediaType));
            append("\n");
            return this;
        }

        SseEventBuilderImpl append(String text) {
            if (this.sb == null) {
                this.sb = new StringBuilder();
            }
            this.sb.append(text);
            return this;
        }

        @Override
        public Set<DataWithMediaType> build() {
            if (!StringUtils.hasLength(this.sb) && this.dataToSend.isEmpty()) {
                return Collections.emptySet();
            }
            append("\n");
            saveAppendedText();
            return this.dataToSend;
        }

        private void saveAppendedText() {
            if (this.sb != null) {
                this.dataToSend.add(new DataWithMediaType(this.sb.toString(), TEXT_PLAIN));
                this.sb = null;
            }
        }

    }

}
