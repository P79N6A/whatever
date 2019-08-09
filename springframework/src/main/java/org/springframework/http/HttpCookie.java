package org.springframework.http;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

public class HttpCookie {

    private final String name;

    private final String value;

    public HttpCookie(String name, @Nullable String value) {
        Assert.hasLength(name, "'name' is required and must not be empty.");
        this.name = name;
        this.value = (value != null ? value : "");
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof HttpCookie)) {
            return false;
        }
        HttpCookie otherCookie = (HttpCookie) other;
        return (this.name.equalsIgnoreCase(otherCookie.getName()));
    }

    @Override
    public String toString() {
        return this.name + '=' + this.value;
    }

}
