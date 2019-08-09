package org.springframework.web.client;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExtractingResponseErrorHandler extends DefaultResponseErrorHandler {

    private List<HttpMessageConverter<?>> messageConverters = Collections.emptyList();

    private final Map<HttpStatus, Class<? extends RestClientException>> statusMapping = new LinkedHashMap<>();

    private final Map<HttpStatus.Series, Class<? extends RestClientException>> seriesMapping = new LinkedHashMap<>();

    public ExtractingResponseErrorHandler() {
    }

    public ExtractingResponseErrorHandler(List<HttpMessageConverter<?>> messageConverters) {
        this.messageConverters = messageConverters;
    }

    public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
        this.messageConverters = messageConverters;
    }

    public void setStatusMapping(Map<HttpStatus, Class<? extends RestClientException>> statusMapping) {
        if (!CollectionUtils.isEmpty(statusMapping)) {
            this.statusMapping.putAll(statusMapping);
        }
    }

    public void setSeriesMapping(Map<HttpStatus.Series, Class<? extends RestClientException>> seriesMapping) {
        if (!CollectionUtils.isEmpty(seriesMapping)) {
            this.seriesMapping.putAll(seriesMapping);
        }
    }

    @Override
    protected boolean hasError(HttpStatus statusCode) {
        if (this.statusMapping.containsKey(statusCode)) {
            return this.statusMapping.get(statusCode) != null;
        } else if (this.seriesMapping.containsKey(statusCode.series())) {
            return this.seriesMapping.get(statusCode.series()) != null;
        } else {
            return super.hasError(statusCode);
        }
    }

    @Override
    public void handleError(ClientHttpResponse response, HttpStatus statusCode) throws IOException {
        if (this.statusMapping.containsKey(statusCode)) {
            extract(this.statusMapping.get(statusCode), response);
        } else if (this.seriesMapping.containsKey(statusCode.series())) {
            extract(this.seriesMapping.get(statusCode.series()), response);
        } else {
            super.handleError(response, statusCode);
        }
    }

    private void extract(@Nullable Class<? extends RestClientException> exceptionClass, ClientHttpResponse response) throws IOException {
        if (exceptionClass == null) {
            return;
        }
        HttpMessageConverterExtractor<? extends RestClientException> extractor = new HttpMessageConverterExtractor<>(exceptionClass, this.messageConverters);
        RestClientException exception = extractor.extractData(response);
        if (exception != null) {
            throw exception;
        }
    }

}
