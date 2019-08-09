package org.springframework.http;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;

public final class ResponseCookie extends HttpCookie {

    private final Duration maxAge;

    @Nullable
    private final String domain;

    @Nullable
    private final String path;

    private final boolean secure;

    private final boolean httpOnly;

    @Nullable
    private final String sameSite;

    private ResponseCookie(String name, String value, Duration maxAge, @Nullable String domain, @Nullable String path, boolean secure, boolean httpOnly, @Nullable String sameSite) {
        super(name, value);
        Assert.notNull(maxAge, "Max age must not be null");
        this.maxAge = maxAge;
        this.domain = domain;
        this.path = path;
        this.secure = secure;
        this.httpOnly = httpOnly;
        this.sameSite = sameSite;
    }

    public Duration getMaxAge() {
        return this.maxAge;
    }

    @Nullable
    public String getDomain() {
        return this.domain;
    }

    @Nullable
    public String getPath() {
        return this.path;
    }

    public boolean isSecure() {
        return this.secure;
    }

    public boolean isHttpOnly() {
        return this.httpOnly;
    }

    @Nullable
    public String getSameSite() {
        return this.sameSite;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ResponseCookie)) {
            return false;
        }
        ResponseCookie otherCookie = (ResponseCookie) other;
        return (getName().equalsIgnoreCase(otherCookie.getName()) && ObjectUtils.nullSafeEquals(this.path, otherCookie.getPath()) && ObjectUtils.nullSafeEquals(this.domain, otherCookie.getDomain()));
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + ObjectUtils.nullSafeHashCode(this.domain);
        result = 31 * result + ObjectUtils.nullSafeHashCode(this.path);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append('=').append(getValue());
        if (StringUtils.hasText(getPath())) {
            sb.append("; Path=").append(getPath());
        }
        if (StringUtils.hasText(this.domain)) {
            sb.append("; Domain=").append(this.domain);
        }
        if (!this.maxAge.isNegative()) {
            sb.append("; Max-Age=").append(this.maxAge.getSeconds());
            sb.append("; Expires=");
            long millis = this.maxAge.getSeconds() > 0 ? System.currentTimeMillis() + this.maxAge.toMillis() : 0;
            sb.append(HttpHeaders.formatDate(millis));
        }
        if (this.secure) {
            sb.append("; Secure");
        }
        if (this.httpOnly) {
            sb.append("; HttpOnly");
        }
        if (StringUtils.hasText(this.sameSite)) {
            sb.append("; SameSite=").append(this.sameSite);
        }
        return sb.toString();
    }

    public static ResponseCookieBuilder from(final String name, final String value) {
        return new ResponseCookieBuilder() {

            private Duration maxAge = Duration.ofSeconds(-1);

            @Nullable
            private String domain;

            @Nullable
            private String path;

            private boolean secure;

            private boolean httpOnly;

            @Nullable
            private String sameSite;

            @Override
            public ResponseCookieBuilder maxAge(Duration maxAge) {
                this.maxAge = maxAge;
                return this;
            }

            @Override
            public ResponseCookieBuilder maxAge(long maxAgeSeconds) {
                this.maxAge = maxAgeSeconds >= 0 ? Duration.ofSeconds(maxAgeSeconds) : Duration.ofSeconds(-1);
                return this;
            }

            @Override
            public ResponseCookieBuilder domain(String domain) {
                this.domain = domain;
                return this;
            }

            @Override
            public ResponseCookieBuilder path(String path) {
                this.path = path;
                return this;
            }

            @Override
            public ResponseCookieBuilder secure(boolean secure) {
                this.secure = secure;
                return this;
            }

            @Override
            public ResponseCookieBuilder httpOnly(boolean httpOnly) {
                this.httpOnly = httpOnly;
                return this;
            }

            @Override
            public ResponseCookieBuilder sameSite(@Nullable String sameSite) {
                this.sameSite = sameSite;
                return this;
            }

            @Override
            public ResponseCookie build() {
                return new ResponseCookie(name, value, this.maxAge, this.domain, this.path, this.secure, this.httpOnly, this.sameSite);
            }
        };
    }

    public interface ResponseCookieBuilder {

        ResponseCookieBuilder maxAge(Duration maxAge);

        ResponseCookieBuilder maxAge(long maxAgeSeconds);

        ResponseCookieBuilder path(String path);

        ResponseCookieBuilder domain(String domain);

        ResponseCookieBuilder secure(boolean secure);

        ResponseCookieBuilder httpOnly(boolean httpOnly);

        ResponseCookieBuilder sameSite(@Nullable String sameSite);

        ResponseCookie build();

    }

}
