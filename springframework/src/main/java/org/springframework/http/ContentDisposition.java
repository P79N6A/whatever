package org.springframework.http;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

public final class ContentDisposition {

    @Nullable
    private final String type;

    @Nullable
    private final String name;

    @Nullable
    private final String filename;

    @Nullable
    private final Charset charset;

    @Nullable
    private final Long size;

    @Nullable
    private final ZonedDateTime creationDate;

    @Nullable
    private final ZonedDateTime modificationDate;

    @Nullable
    private final ZonedDateTime readDate;

    private ContentDisposition(@Nullable String type, @Nullable String name, @Nullable String filename, @Nullable Charset charset, @Nullable Long size, @Nullable ZonedDateTime creationDate, @Nullable ZonedDateTime modificationDate, @Nullable ZonedDateTime readDate) {
        this.type = type;
        this.name = name;
        this.filename = filename;
        this.charset = charset;
        this.size = size;
        this.creationDate = creationDate;
        this.modificationDate = modificationDate;
        this.readDate = readDate;
    }

    @Nullable
    public String getType() {
        return this.type;
    }

    @Nullable
    public String getName() {
        return this.name;
    }

    @Nullable
    public String getFilename() {
        return this.filename;
    }

    @Nullable
    public Charset getCharset() {
        return this.charset;
    }

    @Nullable
    public Long getSize() {
        return this.size;
    }

    @Nullable
    public ZonedDateTime getCreationDate() {
        return this.creationDate;
    }

    @Nullable
    public ZonedDateTime getModificationDate() {
        return this.modificationDate;
    }

    @Nullable
    public ZonedDateTime getReadDate() {
        return this.readDate;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ContentDisposition)) {
            return false;
        }
        ContentDisposition otherCd = (ContentDisposition) other;
        return (ObjectUtils.nullSafeEquals(this.type, otherCd.type) && ObjectUtils.nullSafeEquals(this.name, otherCd.name) && ObjectUtils.nullSafeEquals(this.filename, otherCd.filename) && ObjectUtils.nullSafeEquals(this.charset, otherCd.charset) && ObjectUtils.nullSafeEquals(this.size, otherCd.size) && ObjectUtils.nullSafeEquals(this.creationDate, otherCd.creationDate) && ObjectUtils.nullSafeEquals(this.modificationDate, otherCd.modificationDate) && ObjectUtils.nullSafeEquals(this.readDate, otherCd.readDate));
    }

    @Override
    public int hashCode() {
        int result = ObjectUtils.nullSafeHashCode(this.type);
        result = 31 * result + ObjectUtils.nullSafeHashCode(this.name);
        result = 31 * result + ObjectUtils.nullSafeHashCode(this.filename);
        result = 31 * result + ObjectUtils.nullSafeHashCode(this.charset);
        result = 31 * result + ObjectUtils.nullSafeHashCode(this.size);
        result = 31 * result + (this.creationDate != null ? this.creationDate.hashCode() : 0);
        result = 31 * result + (this.modificationDate != null ? this.modificationDate.hashCode() : 0);
        result = 31 * result + (this.readDate != null ? this.readDate.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.type != null) {
            sb.append(this.type);
        }
        if (this.name != null) {
            sb.append("; name=\"");
            sb.append(this.name).append('\"');
        }
        if (this.filename != null) {
            if (this.charset == null || StandardCharsets.US_ASCII.equals(this.charset)) {
                sb.append("; filename=\"");
                sb.append(this.filename).append('\"');
            } else {
                sb.append("; filename*=");
                sb.append(encodeHeaderFieldParam(this.filename, this.charset));
            }
        }
        if (this.size != null) {
            sb.append("; size=");
            sb.append(this.size);
        }
        if (this.creationDate != null) {
            sb.append("; creation-date=\"");
            sb.append(RFC_1123_DATE_TIME.format(this.creationDate));
            sb.append('\"');
        }
        if (this.modificationDate != null) {
            sb.append("; modification-date=\"");
            sb.append(RFC_1123_DATE_TIME.format(this.modificationDate));
            sb.append('\"');
        }
        if (this.readDate != null) {
            sb.append("; read-date=\"");
            sb.append(RFC_1123_DATE_TIME.format(this.readDate));
            sb.append('\"');
        }
        return sb.toString();
    }

    public static Builder builder(String type) {
        return new BuilderImpl(type);
    }

    public static ContentDisposition empty() {
        return new ContentDisposition("", null, null, null, null, null, null, null);
    }

    public static ContentDisposition parse(String contentDisposition) {
        List<String> parts = tokenize(contentDisposition);
        String type = parts.get(0);
        String name = null;
        String filename = null;
        Charset charset = null;
        Long size = null;
        ZonedDateTime creationDate = null;
        ZonedDateTime modificationDate = null;
        ZonedDateTime readDate = null;
        for (int i = 1; i < parts.size(); i++) {
            String part = parts.get(i);
            int eqIndex = part.indexOf('=');
            if (eqIndex != -1) {
                String attribute = part.substring(0, eqIndex);
                String value = (part.startsWith("\"", eqIndex + 1) && part.endsWith("\"") ? part.substring(eqIndex + 2, part.length() - 1) : part.substring(eqIndex + 1, part.length()));
                if (attribute.equals("name")) {
                    name = value;
                } else if (attribute.equals("filename*")) {
                    filename = decodeHeaderFieldParam(value);
                    charset = Charset.forName(value.substring(0, value.indexOf('\'')));
                    Assert.isTrue(UTF_8.equals(charset) || ISO_8859_1.equals(charset), "Charset should be UTF-8 or ISO-8859-1");
                } else if (attribute.equals("filename") && (filename == null)) {
                    filename = value;
                } else if (attribute.equals("size")) {
                    size = Long.parseLong(value);
                } else if (attribute.equals("creation-date")) {
                    try {
                        creationDate = ZonedDateTime.parse(value, RFC_1123_DATE_TIME);
                    } catch (DateTimeParseException ex) {
                        // ignore
                    }
                } else if (attribute.equals("modification-date")) {
                    try {
                        modificationDate = ZonedDateTime.parse(value, RFC_1123_DATE_TIME);
                    } catch (DateTimeParseException ex) {
                        // ignore
                    }
                } else if (attribute.equals("read-date")) {
                    try {
                        readDate = ZonedDateTime.parse(value, RFC_1123_DATE_TIME);
                    } catch (DateTimeParseException ex) {
                        // ignore
                    }
                }
            } else {
                throw new IllegalArgumentException("Invalid content disposition format");
            }
        }
        return new ContentDisposition(type, name, filename, charset, size, creationDate, modificationDate, readDate);
    }

    private static List<String> tokenize(String headerValue) {
        int index = headerValue.indexOf(';');
        String type = (index >= 0 ? headerValue.substring(0, index) : headerValue).trim();
        if (type.isEmpty()) {
            throw new IllegalArgumentException("Content-Disposition header must not be empty");
        }
        List<String> parts = new ArrayList<>();
        parts.add(type);
        if (index >= 0) {
            do {
                int nextIndex = index + 1;
                boolean quoted = false;
                while (nextIndex < headerValue.length()) {
                    char ch = headerValue.charAt(nextIndex);
                    if (ch == ';') {
                        if (!quoted) {
                            break;
                        }
                    } else if (ch == '"') {
                        quoted = !quoted;
                    }
                    nextIndex++;
                }
                String part = headerValue.substring(index + 1, nextIndex).trim();
                if (!part.isEmpty()) {
                    parts.add(part);
                }
                index = nextIndex;
            } while (index < headerValue.length());
        }
        return parts;
    }

    private static String decodeHeaderFieldParam(String input) {
        Assert.notNull(input, "Input String should not be null");
        int firstQuoteIndex = input.indexOf('\'');
        int secondQuoteIndex = input.indexOf('\'', firstQuoteIndex + 1);
        // US_ASCII
        if (firstQuoteIndex == -1 || secondQuoteIndex == -1) {
            return input;
        }
        Charset charset = Charset.forName(input.substring(0, firstQuoteIndex));
        Assert.isTrue(UTF_8.equals(charset) || ISO_8859_1.equals(charset), "Charset should be UTF-8 or ISO-8859-1");
        byte[] value = input.substring(secondQuoteIndex + 1, input.length()).getBytes(charset);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int index = 0;
        while (index < value.length) {
            byte b = value[index];
            if (isRFC5987AttrChar(b)) {
                bos.write((char) b);
                index++;
            } else if (b == '%') {
                char[] array = {(char) value[index + 1], (char) value[index + 2]};
                bos.write(Integer.parseInt(String.valueOf(array), 16));
                index += 3;
            } else {
                throw new IllegalArgumentException("Invalid header field parameter format (as defined in RFC 5987)");
            }
        }
        return new String(bos.toByteArray(), charset);
    }

    private static boolean isRFC5987AttrChar(byte c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '!' || c == '#' || c == '$' || c == '&' || c == '+' || c == '-' || c == '.' || c == '^' || c == '_' || c == '`' || c == '|' || c == '~';
    }

    private static String encodeHeaderFieldParam(String input, Charset charset) {
        Assert.notNull(input, "Input String should not be null");
        Assert.notNull(charset, "Charset should not be null");
        if (StandardCharsets.US_ASCII.equals(charset)) {
            return input;
        }
        Assert.isTrue(UTF_8.equals(charset) || ISO_8859_1.equals(charset), "Charset should be UTF-8 or ISO-8859-1");
        byte[] source = input.getBytes(charset);
        int len = source.length;
        StringBuilder sb = new StringBuilder(len << 1);
        sb.append(charset.name());
        sb.append("''");
        for (byte b : source) {
            if (isRFC5987AttrChar(b)) {
                sb.append((char) b);
            } else {
                sb.append('%');
                char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
                char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
                sb.append(hex1);
                sb.append(hex2);
            }
        }
        return sb.toString();
    }

    public interface Builder {

        Builder name(String name);

        Builder filename(String filename);

        Builder filename(String filename, Charset charset);

        Builder size(Long size);

        Builder creationDate(ZonedDateTime creationDate);

        Builder modificationDate(ZonedDateTime modificationDate);

        Builder readDate(ZonedDateTime readDate);

        ContentDisposition build();

    }

    private static class BuilderImpl implements Builder {

        private String type;

        @Nullable
        private String name;

        @Nullable
        private String filename;

        @Nullable
        private Charset charset;

        @Nullable
        private Long size;

        @Nullable
        private ZonedDateTime creationDate;

        @Nullable
        private ZonedDateTime modificationDate;

        @Nullable
        private ZonedDateTime readDate;

        public BuilderImpl(String type) {
            Assert.hasText(type, "'type' must not be not empty");
            this.type = type;
        }

        @Override
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }

        @Override
        public Builder filename(String filename, Charset charset) {
            this.filename = filename;
            this.charset = charset;
            return this;
        }

        @Override
        public Builder size(Long size) {
            this.size = size;
            return this;
        }

        @Override
        public Builder creationDate(ZonedDateTime creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        @Override
        public Builder modificationDate(ZonedDateTime modificationDate) {
            this.modificationDate = modificationDate;
            return this;
        }

        @Override
        public Builder readDate(ZonedDateTime readDate) {
            this.readDate = readDate;
            return this;
        }

        @Override
        public ContentDisposition build() {
            return new ContentDisposition(this.type, this.name, this.filename, this.charset, this.size, this.creationDate, this.modificationDate, this.readDate);
        }

    }

}
