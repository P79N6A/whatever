package org.springframework.web.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class CookieGenerator {

    public static final String DEFAULT_COOKIE_PATH = "/";

    protected final Log logger = LogFactory.getLog(getClass());

    @Nullable
    private String cookieName;

    @Nullable
    private String cookieDomain;

    private String cookiePath = DEFAULT_COOKIE_PATH;

    @Nullable
    private Integer cookieMaxAge;

    private boolean cookieSecure = false;

    private boolean cookieHttpOnly = false;

    public void setCookieName(@Nullable String cookieName) {
        this.cookieName = cookieName;
    }

    @Nullable
    public String getCookieName() {
        return this.cookieName;
    }

    public void setCookieDomain(@Nullable String cookieDomain) {
        this.cookieDomain = cookieDomain;
    }

    @Nullable
    public String getCookieDomain() {
        return this.cookieDomain;
    }

    public void setCookiePath(String cookiePath) {
        this.cookiePath = cookiePath;
    }

    public String getCookiePath() {
        return this.cookiePath;
    }

    public void setCookieMaxAge(@Nullable Integer cookieMaxAge) {
        this.cookieMaxAge = cookieMaxAge;
    }

    @Nullable
    public Integer getCookieMaxAge() {
        return this.cookieMaxAge;
    }

    public void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public boolean isCookieSecure() {
        return this.cookieSecure;
    }

    public void setCookieHttpOnly(boolean cookieHttpOnly) {
        this.cookieHttpOnly = cookieHttpOnly;
    }

    public boolean isCookieHttpOnly() {
        return this.cookieHttpOnly;
    }

    public void addCookie(HttpServletResponse response, String cookieValue) {
        Assert.notNull(response, "HttpServletResponse must not be null");
        Cookie cookie = createCookie(cookieValue);
        Integer maxAge = getCookieMaxAge();
        if (maxAge != null) {
            cookie.setMaxAge(maxAge);
        }
        if (isCookieSecure()) {
            cookie.setSecure(true);
        }
        if (isCookieHttpOnly()) {
            cookie.setHttpOnly(true);
        }
        response.addCookie(cookie);
        if (logger.isTraceEnabled()) {
            logger.trace("Added cookie [" + getCookieName() + "=" + cookieValue + "]");
        }
    }

    public void removeCookie(HttpServletResponse response) {
        Assert.notNull(response, "HttpServletResponse must not be null");
        Cookie cookie = createCookie("");
        cookie.setMaxAge(0);
        if (isCookieSecure()) {
            cookie.setSecure(true);
        }
        if (isCookieHttpOnly()) {
            cookie.setHttpOnly(true);
        }
        response.addCookie(cookie);
        if (logger.isTraceEnabled()) {
            logger.trace("Removed cookie '" + getCookieName() + "'");
        }
    }

    protected Cookie createCookie(String cookieValue) {
        Cookie cookie = new Cookie(getCookieName(), cookieValue);
        if (getCookieDomain() != null) {
            cookie.setDomain(getCookieDomain());
        }
        cookie.setPath(getCookiePath());
        return cookie;
    }

}
