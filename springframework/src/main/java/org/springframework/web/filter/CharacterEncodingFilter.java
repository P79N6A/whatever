package org.springframework.web.filter;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CharacterEncodingFilter extends OncePerRequestFilter {

    @Nullable
    private String encoding;

    private boolean forceRequestEncoding = false;

    private boolean forceResponseEncoding = false;

    public CharacterEncodingFilter() {
    }

    public CharacterEncodingFilter(String encoding) {
        this(encoding, false);
    }

    public CharacterEncodingFilter(String encoding, boolean forceEncoding) {
        this(encoding, forceEncoding, forceEncoding);
    }

    public CharacterEncodingFilter(String encoding, boolean forceRequestEncoding, boolean forceResponseEncoding) {
        Assert.hasLength(encoding, "Encoding must not be empty");
        this.encoding = encoding;
        this.forceRequestEncoding = forceRequestEncoding;
        this.forceResponseEncoding = forceResponseEncoding;
    }

    public void setEncoding(@Nullable String encoding) {
        this.encoding = encoding;
    }

    @Nullable
    public String getEncoding() {
        return this.encoding;
    }

    public void setForceEncoding(boolean forceEncoding) {
        this.forceRequestEncoding = forceEncoding;
        this.forceResponseEncoding = forceEncoding;
    }

    public void setForceRequestEncoding(boolean forceRequestEncoding) {
        this.forceRequestEncoding = forceRequestEncoding;
    }

    public boolean isForceRequestEncoding() {
        return this.forceRequestEncoding;
    }

    public void setForceResponseEncoding(boolean forceResponseEncoding) {
        this.forceResponseEncoding = forceResponseEncoding;
    }

    public boolean isForceResponseEncoding() {
        return this.forceResponseEncoding;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String encoding = getEncoding();
        if (encoding != null) {
            if (isForceRequestEncoding() || request.getCharacterEncoding() == null) {
                request.setCharacterEncoding(encoding);
            }
            if (isForceResponseEncoding()) {
                response.setCharacterEncoding(encoding);
            }
        }
        filterChain.doFilter(request, response);
    }

}
