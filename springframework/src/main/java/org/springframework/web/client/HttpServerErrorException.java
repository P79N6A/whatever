package org.springframework.web.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

import java.nio.charset.Charset;

public class HttpServerErrorException extends HttpStatusCodeException {

    private static final long serialVersionUID = -2915754006618138282L;

    public HttpServerErrorException(HttpStatus statusCode) {
        super(statusCode);
    }

    public HttpServerErrorException(HttpStatus statusCode, String statusText) {
        super(statusCode, statusText);
    }

    public HttpServerErrorException(HttpStatus statusCode, String statusText, @Nullable byte[] body, @Nullable Charset charset) {
        super(statusCode, statusText, body, charset);
    }

    public HttpServerErrorException(HttpStatus statusCode, String statusText, @Nullable HttpHeaders headers, @Nullable byte[] body, @Nullable Charset charset) {
        super(statusCode, statusText, headers, body, charset);
    }

    public static HttpServerErrorException create(HttpStatus statusCode, String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
        switch (statusCode) {
            case INTERNAL_SERVER_ERROR:
                return new HttpServerErrorException.InternalServerError(statusText, headers, body, charset);
            case NOT_IMPLEMENTED:
                return new HttpServerErrorException.NotImplemented(statusText, headers, body, charset);
            case BAD_GATEWAY:
                return new HttpServerErrorException.BadGateway(statusText, headers, body, charset);
            case SERVICE_UNAVAILABLE:
                return new HttpServerErrorException.ServiceUnavailable(statusText, headers, body, charset);
            case GATEWAY_TIMEOUT:
                return new HttpServerErrorException.GatewayTimeout(statusText, headers, body, charset);
            default:
                return new HttpServerErrorException(statusCode, statusText, headers, body, charset);
        }
    }
    // Subclasses for specific HTTP status codes

    @SuppressWarnings("serial")
    public static class InternalServerError extends HttpServerErrorException {

        InternalServerError(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
            super(HttpStatus.INTERNAL_SERVER_ERROR, statusText, headers, body, charset);
        }

    }

    @SuppressWarnings("serial")
    public static class NotImplemented extends HttpServerErrorException {

        NotImplemented(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
            super(HttpStatus.NOT_IMPLEMENTED, statusText, headers, body, charset);
        }

    }

    @SuppressWarnings("serial")
    public static class BadGateway extends HttpServerErrorException {

        BadGateway(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
            super(HttpStatus.BAD_GATEWAY, statusText, headers, body, charset);
        }

    }

    @SuppressWarnings("serial")
    public static class ServiceUnavailable extends HttpServerErrorException {

        ServiceUnavailable(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
            super(HttpStatus.SERVICE_UNAVAILABLE, statusText, headers, body, charset);
        }

    }

    @SuppressWarnings("serial")
    public static class GatewayTimeout extends HttpServerErrorException {

        GatewayTimeout(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
            super(HttpStatus.GATEWAY_TIMEOUT, statusText, headers, body, charset);
        }

    }

}
