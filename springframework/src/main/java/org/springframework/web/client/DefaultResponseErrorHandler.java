package org.springframework.web.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.charset.Charset;

public class DefaultResponseErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        int rawStatusCode = response.getRawStatusCode();
        HttpStatus statusCode = HttpStatus.resolve(rawStatusCode);
        return (statusCode != null ? hasError(statusCode) : hasError(rawStatusCode));
    }

    protected boolean hasError(HttpStatus statusCode) {
        return statusCode.isError();
    }

    protected boolean hasError(int unknownStatusCode) {
        HttpStatus.Series series = HttpStatus.Series.resolve(unknownStatusCode);
        return (series == HttpStatus.Series.CLIENT_ERROR || series == HttpStatus.Series.SERVER_ERROR);
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = HttpStatus.resolve(response.getRawStatusCode());
        if (statusCode == null) {
            throw new UnknownHttpStatusCodeException(response.getRawStatusCode(), response.getStatusText(), response.getHeaders(), getResponseBody(response), getCharset(response));
        }
        handleError(response, statusCode);
    }

    protected void handleError(ClientHttpResponse response, HttpStatus statusCode) throws IOException {
        String statusText = response.getStatusText();
        HttpHeaders headers = response.getHeaders();
        byte[] body = getResponseBody(response);
        Charset charset = getCharset(response);
        switch (statusCode.series()) {
            case CLIENT_ERROR:
                throw HttpClientErrorException.create(statusCode, statusText, headers, body, charset);
            case SERVER_ERROR:
                throw HttpServerErrorException.create(statusCode, statusText, headers, body, charset);
            default:
                throw new UnknownHttpStatusCodeException(statusCode.value(), statusText, headers, body, charset);
        }
    }

    @Deprecated
    protected HttpStatus getHttpStatusCode(ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = HttpStatus.resolve(response.getRawStatusCode());
        if (statusCode == null) {
            throw new UnknownHttpStatusCodeException(response.getRawStatusCode(), response.getStatusText(), response.getHeaders(), getResponseBody(response), getCharset(response));
        }
        return statusCode;
    }

    protected byte[] getResponseBody(ClientHttpResponse response) {
        try {
            return FileCopyUtils.copyToByteArray(response.getBody());
        } catch (IOException ex) {
            // ignore
        }
        return new byte[0];
    }

    @Nullable
    protected Charset getCharset(ClientHttpResponse response) {
        HttpHeaders headers = response.getHeaders();
        MediaType contentType = headers.getContentType();
        return (contentType != null ? contentType.getCharset() : null);
    }

}
