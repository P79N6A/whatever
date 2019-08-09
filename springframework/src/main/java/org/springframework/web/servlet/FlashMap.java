package org.springframework.web.servlet;

import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;

@SuppressWarnings("serial")
public final class FlashMap extends HashMap<String, Object> implements Comparable<FlashMap> {

    @Nullable
    private String targetRequestPath;

    private final MultiValueMap<String, String> targetRequestParams = new LinkedMultiValueMap<>(4);

    private long expirationTime = -1;

    public void setTargetRequestPath(@Nullable String path) {
        this.targetRequestPath = path;
    }

    @Nullable
    public String getTargetRequestPath() {
        return this.targetRequestPath;
    }

    public FlashMap addTargetRequestParams(@Nullable MultiValueMap<String, String> params) {
        if (params != null) {
            params.forEach((key, values) -> {
                for (String value : values) {
                    addTargetRequestParam(key, value);
                }
            });
        }
        return this;
    }

    public FlashMap addTargetRequestParam(String name, String value) {
        if (StringUtils.hasText(name) && StringUtils.hasText(value)) {
            this.targetRequestParams.add(name, value);
        }
        return this;
    }

    public MultiValueMap<String, String> getTargetRequestParams() {
        return this.targetRequestParams;
    }

    public void startExpirationPeriod(int timeToLive) {
        this.expirationTime = System.currentTimeMillis() + timeToLive * 1000;
    }

    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
    }

    public long getExpirationTime() {
        return this.expirationTime;
    }

    public boolean isExpired() {
        return (this.expirationTime != -1 && System.currentTimeMillis() > this.expirationTime);
    }

    @Override
    public int compareTo(FlashMap other) {
        int thisUrlPath = (this.targetRequestPath != null ? 1 : 0);
        int otherUrlPath = (other.targetRequestPath != null ? 1 : 0);
        if (thisUrlPath != otherUrlPath) {
            return otherUrlPath - thisUrlPath;
        } else {
            return other.targetRequestParams.size() - this.targetRequestParams.size();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FlashMap)) {
            return false;
        }
        FlashMap otherFlashMap = (FlashMap) other;
        return (super.equals(otherFlashMap) && ObjectUtils.nullSafeEquals(this.targetRequestPath, otherFlashMap.targetRequestPath) && this.targetRequestParams.equals(otherFlashMap.targetRequestParams));
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + ObjectUtils.nullSafeHashCode(this.targetRequestPath);
        result = 31 * result + this.targetRequestParams.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "FlashMap [attributes=" + super.toString() + ", targetRequestPath=" + this.targetRequestPath + ", targetRequestParams=" + this.targetRequestParams + "]";
    }

}
