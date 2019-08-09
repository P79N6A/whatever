package org.springframework.web.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

import java.nio.charset.Charset;

public class HttpClientErrorException extends HttpStatusCodeException {

    private static final long serialVersionUID = 5177019431887513952L;

    public HttpClientErrorException(HttpStatus statusCode) {
        super(statusCode);
    }

    public HttpClientErrorException(HttpStatus statusCode, String statusText) {
        super(statusCode, statusText);
    }

    public HttpClientErrorException(HttpStatus statusCode, String statusText, @Nullable byte[] body, @Nullable Charset responseCharset) {
        super(statusCode, statusText, body, responseCharset);
    }

    public HttpClientErrorException(HttpStatus statusCode, String statusText, @Nullable HttpHeaders headers, @Nullable byte[] body, @Nullable Charset responseCharset) {
        super(statusCode, statusText, headers, body, responseCharset);
    }

    public static HttpClientErrorException create(HttpStatus statusCode, String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
        switch (statusCode) {
            case BAD_REQUEST:
                return new HttpClientErrorException.BadRequest(statusText, headers, body, charset);
            case UNAUTHORIZED:
                return new HttpClientErrorException.Unauthorized(statusText, headers, body, charset);
            case FORBIDDEN:
                return new HttpClientErrorException.Forbidden(statusText, headers, body, charset);
            case NOT_FOUND:
                return new HttpClientErrorException.NotFound(statusText, headers, body, charset);
            case METHOD_NOT_ALLOWED:
                return new HttpClientErrorException.MethodNotAllowed(statusText, headers, body, charset);
            case NOT_ACCEPTABLE:
                return new HttpClientErrorException.NotAcceptable(statusText, headers, body, charset);
            case CONFLICT:
                return new HttpClientErrorException.Conflict(statusText, headers, body, charset);
            case GONE:
                return new HttpClientErrorException.Gone(statusText, headers, body, charset);
            case UNSUPPORTED_MEDIA_TYPE:
                return new HttpClientErrorException.UnsupportedMediaType(statusText, headers, body, charset);
            case TOO_MANY_REQUESTS:
                return new HttpClientErrorException.TooManyRequests(statusText, headers, body, charset);
            case UNPROCESSABLE_ENTITY:
                return new HttpClientErrorException.UnprocessableEntity(statusText, headers, body, charset);
            default:
                return new HttpClientErrorException(statusCode, statusText, headers, body, charset);
        }
    }
    // Subclasses for specific HTTP status codes

    @SuppressWarnings("serial")
    public static class BadRequest extends HttpClientErrorException {

        BadRequest(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
            super(HttpStatus.BAD_REQUEST, statusText, headers, body, charset);
        }

    }

    @SuppressWarnings("serial")
    public static class Unauthorized extends HttpClientErrorException {

        Unauthorized(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
            super(HttpStatus.UNAUTHORIZED, statusText, headers, body, charset);
        }

    }

    @SuppressWarnings("serial")
    public static class Forbidden extends HttpClientErrorException {

        Forbidden(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
            super(HttpStatus.FORBIDDEN, statusText, headers, body, charset);
        }

    }

    @SuppressWarnings("serial")
    public static class NotFound extends HttpClientErrorException {

        NotFound(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
            super(HttpStatus.NOT_FOUND, statusText, headers, body, charset);
        }

    }

    @SuppressWarnings("serial")
    public static class MethodNotAllowed extends HttpClientErrorException {

        MethodNotAllowed(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
            super(HttpStatus.METHOD_NOT_ALLOWED, statusText, headers, body, charset);
        }

    }

    @SuppressWarnings("serial")
    public static class NotAcceptable extends HttpClientErrorException {

        NotAcceptable(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
            super(HttpStatus.NOT_ACCEPTABLE, statusText, headers, body, charset);
        }

    }

    @SuppressWarnings("serial")
    public static class Conflict extends HttpClientErrorException {

        Conflict(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
            super(HttpStatus.CONFLICT, statusText, headers, body, charset);
        }

    }

    @SuppressWarnings("serial")
    public static class Gone extends HttpClientErrorException {

        Gone(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
            super(HttpStatus.GONE, statusText, headers, body, charset);
        }

    }

    @SuppressWarnings("serial")
    public static class UnsupportedMediaType extends HttpClientErrorException {

        UnsupportedMediaType(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
            super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusText, headers, body, charset);
        }

    }

    @SuppressWarnings("serial")
    public static class UnprocessableEntity extends HttpClientErrorException {

        UnprocessableEntity(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
            super(HttpStatus.UNPROCESSABLE_ENTITY, statusText, headers, body, charset);
        }

    }

    @SuppressWarnings("serial")
    public static class TooManyRequests extends HttpClientErrorException {

        TooManyRequests(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
            super(HttpStatus.TOO_MANY_REQUESTS, statusText, headers, body, charset);
        }

    }

}
