package org.springframework.web.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.InterceptingHttpAccessor;
import org.springframework.http.converter.*;
import org.springframework.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.JsonbHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.DefaultUriBuilderFactory.EncodingMode;
import org.springframework.web.util.UriTemplateHandler;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RestTemplate extends InterceptingHttpAccessor implements RestOperations {

    private static boolean romePresent;

    private static final boolean jaxb2Present;

    private static final boolean jackson2Present;

    private static final boolean jackson2XmlPresent;

    private static final boolean jackson2SmilePresent;

    private static final boolean jackson2CborPresent;

    private static final boolean gsonPresent;

    private static final boolean jsonbPresent;

    static {
        ClassLoader classLoader = RestTemplate.class.getClassLoader();
        romePresent = ClassUtils.isPresent("com.rometools.rome.feed.WireFeed", classLoader);
        jaxb2Present = ClassUtils.isPresent("javax.xml.bind.Binder", classLoader);
        jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) && ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
        jackson2XmlPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", classLoader);
        jackson2SmilePresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);
        jackson2CborPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.cbor.CBORFactory", classLoader);
        gsonPresent = ClassUtils.isPresent("com.google.gson.Gson", classLoader);
        jsonbPresent = ClassUtils.isPresent("javax.json.bind.Jsonb", classLoader);
    }

    private final List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();

    private ResponseErrorHandler errorHandler = new DefaultResponseErrorHandler();

    private UriTemplateHandler uriTemplateHandler;

    private final ResponseExtractor<HttpHeaders> headersExtractor = new HeadersExtractor();

    public RestTemplate() {
        this.messageConverters.add(new ByteArrayHttpMessageConverter());
        this.messageConverters.add(new StringHttpMessageConverter());
        this.messageConverters.add(new ResourceHttpMessageConverter(false));
        try {
            this.messageConverters.add(new SourceHttpMessageConverter<>());
        } catch (Error err) {
            // Ignore when no TransformerFactory implementation is available
        }
        this.messageConverters.add(new AllEncompassingFormHttpMessageConverter());
        if (romePresent) {
            this.messageConverters.add(new AtomFeedHttpMessageConverter());
            this.messageConverters.add(new RssChannelHttpMessageConverter());
        }
        if (jackson2XmlPresent) {
            this.messageConverters.add(new MappingJackson2XmlHttpMessageConverter());
        } else if (jaxb2Present) {
            this.messageConverters.add(new Jaxb2RootElementHttpMessageConverter());
        }
        if (jackson2Present) {
            this.messageConverters.add(new MappingJackson2HttpMessageConverter());
        } else if (gsonPresent) {
            this.messageConverters.add(new GsonHttpMessageConverter());
        } else if (jsonbPresent) {
            this.messageConverters.add(new JsonbHttpMessageConverter());
        }
        if (jackson2SmilePresent) {
            this.messageConverters.add(new MappingJackson2SmileHttpMessageConverter());
        }
        if (jackson2CborPresent) {
            this.messageConverters.add(new MappingJackson2CborHttpMessageConverter());
        }
        this.uriTemplateHandler = initUriTemplateHandler();
    }

    public RestTemplate(ClientHttpRequestFactory requestFactory) {
        this();
        setRequestFactory(requestFactory);
    }

    public RestTemplate(List<HttpMessageConverter<?>> messageConverters) {
        Assert.notEmpty(messageConverters, "At least one HttpMessageConverter required");
        this.messageConverters.addAll(messageConverters);
        this.uriTemplateHandler = initUriTemplateHandler();
    }

    private static DefaultUriBuilderFactory initUriTemplateHandler() {
        DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory();
        uriFactory.setEncodingMode(EncodingMode.URI_COMPONENT);  // for backwards compatibility..
        return uriFactory;
    }

    public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
        Assert.notEmpty(messageConverters, "At least one HttpMessageConverter required");
        // Take getMessageConverters() List as-is when passed in here
        if (this.messageConverters != messageConverters) {
            this.messageConverters.clear();
            this.messageConverters.addAll(messageConverters);
        }
    }

    public List<HttpMessageConverter<?>> getMessageConverters() {
        return this.messageConverters;
    }

    public void setErrorHandler(ResponseErrorHandler errorHandler) {
        Assert.notNull(errorHandler, "ResponseErrorHandler must not be null");
        this.errorHandler = errorHandler;
    }

    public ResponseErrorHandler getErrorHandler() {
        return this.errorHandler;
    }

    @SuppressWarnings("deprecation")
    public void setDefaultUriVariables(Map<String, ?> uriVars) {
        if (this.uriTemplateHandler instanceof DefaultUriBuilderFactory) {
            ((DefaultUriBuilderFactory) this.uriTemplateHandler).setDefaultUriVariables(uriVars);
        } else if (this.uriTemplateHandler instanceof org.springframework.web.util.AbstractUriTemplateHandler) {
            ((org.springframework.web.util.AbstractUriTemplateHandler) this.uriTemplateHandler).setDefaultUriVariables(uriVars);
        } else {
            throw new IllegalArgumentException("This property is not supported with the configured UriTemplateHandler.");
        }
    }

    public void setUriTemplateHandler(UriTemplateHandler handler) {
        Assert.notNull(handler, "UriTemplateHandler must not be null");
        this.uriTemplateHandler = handler;
    }

    public UriTemplateHandler getUriTemplateHandler() {
        return this.uriTemplateHandler;
    }
    // GET

    @Override
    @Nullable
    public <T> T getForObject(String url, Class<T> responseType, Object... uriVariables) throws RestClientException {
        RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
        HttpMessageConverterExtractor<T> responseExtractor = new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
        return execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables);
    }

    @Override
    @Nullable
    public <T> T getForObject(String url, Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {
        RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
        HttpMessageConverterExtractor<T> responseExtractor = new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
        return execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables);
    }

    @Override
    @Nullable
    public <T> T getForObject(URI url, Class<T> responseType) throws RestClientException {
        RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
        HttpMessageConverterExtractor<T> responseExtractor = new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
        return execute(url, HttpMethod.GET, requestCallback, responseExtractor);
    }

    @Override
    public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Object... uriVariables) throws RestClientException {
        RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
        ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
        return nonNull(execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables));
    }

    @Override
    public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {
        RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
        ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
        return nonNull(execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables));
    }

    @Override
    public <T> ResponseEntity<T> getForEntity(URI url, Class<T> responseType) throws RestClientException {
        RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
        ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
        return nonNull(execute(url, HttpMethod.GET, requestCallback, responseExtractor));
    }
    // HEAD

    @Override
    public HttpHeaders headForHeaders(String url, Object... uriVariables) throws RestClientException {
        return nonNull(execute(url, HttpMethod.HEAD, null, headersExtractor(), uriVariables));
    }

    @Override
    public HttpHeaders headForHeaders(String url, Map<String, ?> uriVariables) throws RestClientException {
        return nonNull(execute(url, HttpMethod.HEAD, null, headersExtractor(), uriVariables));
    }

    @Override
    public HttpHeaders headForHeaders(URI url) throws RestClientException {
        return nonNull(execute(url, HttpMethod.HEAD, null, headersExtractor()));
    }
    // POST

    @Override
    @Nullable
    public URI postForLocation(String url, @Nullable Object request, Object... uriVariables) throws RestClientException {
        RequestCallback requestCallback = httpEntityCallback(request);
        HttpHeaders headers = execute(url, HttpMethod.POST, requestCallback, headersExtractor(), uriVariables);
        return (headers != null ? headers.getLocation() : null);
    }

    @Override
    @Nullable
    public URI postForLocation(String url, @Nullable Object request, Map<String, ?> uriVariables) throws RestClientException {
        RequestCallback requestCallback = httpEntityCallback(request);
        HttpHeaders headers = execute(url, HttpMethod.POST, requestCallback, headersExtractor(), uriVariables);
        return (headers != null ? headers.getLocation() : null);
    }

    @Override
    @Nullable
    public URI postForLocation(URI url, @Nullable Object request) throws RestClientException {
        RequestCallback requestCallback = httpEntityCallback(request);
        HttpHeaders headers = execute(url, HttpMethod.POST, requestCallback, headersExtractor());
        return (headers != null ? headers.getLocation() : null);
    }

    @Override
    @Nullable
    public <T> T postForObject(String url, @Nullable Object request, Class<T> responseType, Object... uriVariables) throws RestClientException {
        RequestCallback requestCallback = httpEntityCallback(request, responseType);
        HttpMessageConverterExtractor<T> responseExtractor = new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
        return execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables);
    }

    @Override
    @Nullable
    public <T> T postForObject(String url, @Nullable Object request, Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {
        RequestCallback requestCallback = httpEntityCallback(request, responseType);
        HttpMessageConverterExtractor<T> responseExtractor = new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
        return execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables);
    }

    @Override
    @Nullable
    public <T> T postForObject(URI url, @Nullable Object request, Class<T> responseType) throws RestClientException {
        RequestCallback requestCallback = httpEntityCallback(request, responseType);
        HttpMessageConverterExtractor<T> responseExtractor = new HttpMessageConverterExtractor<>(responseType, getMessageConverters());
        return execute(url, HttpMethod.POST, requestCallback, responseExtractor);
    }

    @Override
    public <T> ResponseEntity<T> postForEntity(String url, @Nullable Object request, Class<T> responseType, Object... uriVariables) throws RestClientException {
        RequestCallback requestCallback = httpEntityCallback(request, responseType);
        ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
        return nonNull(execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables));
    }

    @Override
    public <T> ResponseEntity<T> postForEntity(String url, @Nullable Object request, Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {
        RequestCallback requestCallback = httpEntityCallback(request, responseType);
        ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
        return nonNull(execute(url, HttpMethod.POST, requestCallback, responseExtractor, uriVariables));
    }

    @Override
    public <T> ResponseEntity<T> postForEntity(URI url, @Nullable Object request, Class<T> responseType) throws RestClientException {
        RequestCallback requestCallback = httpEntityCallback(request, responseType);
        ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
        return nonNull(execute(url, HttpMethod.POST, requestCallback, responseExtractor));
    }
    // PUT

    @Override
    public void put(String url, @Nullable Object request, Object... uriVariables) throws RestClientException {
        RequestCallback requestCallback = httpEntityCallback(request);
        execute(url, HttpMethod.PUT, requestCallback, null, uriVariables);
    }

    @Override
    public void put(String url, @Nullable Object request, Map<String, ?> uriVariables) throws RestClientException {
        RequestCallback requestCallback = httpEntityCallback(request);
        execute(url, HttpMethod.PUT, requestCallback, null, uriVariables);
    }

    @Override
    public void put(URI url, @Nullable Object request) throws RestClientException {
        RequestCallback requestCallback = httpEntityCallback(request);
        execute(url, HttpMethod.PUT, requestCallback, null);
    }
    // PATCH

    @Override
    @Nullable
    public <T> T patchForObject(String url, @Nullable Object request, Class<T> responseType, Object... uriVariables) throws RestClientException {
        RequestCallback requestCallback = httpEntityCallback(request, responseType);
        HttpMessageConverterExtractor<T> responseExtractor = new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
        return execute(url, HttpMethod.PATCH, requestCallback, responseExtractor, uriVariables);
    }

    @Override
    @Nullable
    public <T> T patchForObject(String url, @Nullable Object request, Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {
        RequestCallback requestCallback = httpEntityCallback(request, responseType);
        HttpMessageConverterExtractor<T> responseExtractor = new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
        return execute(url, HttpMethod.PATCH, requestCallback, responseExtractor, uriVariables);
    }

    @Override
    @Nullable
    public <T> T patchForObject(URI url, @Nullable Object request, Class<T> responseType) throws RestClientException {
        RequestCallback requestCallback = httpEntityCallback(request, responseType);
        HttpMessageConverterExtractor<T> responseExtractor = new HttpMessageConverterExtractor<>(responseType, getMessageConverters());
        return execute(url, HttpMethod.PATCH, requestCallback, responseExtractor);
    }
    // DELETE

    @Override
    public void delete(String url, Object... uriVariables) throws RestClientException {
        execute(url, HttpMethod.DELETE, null, null, uriVariables);
    }

    @Override
    public void delete(String url, Map<String, ?> uriVariables) throws RestClientException {
        execute(url, HttpMethod.DELETE, null, null, uriVariables);
    }

    @Override
    public void delete(URI url) throws RestClientException {
        execute(url, HttpMethod.DELETE, null, null);
    }
    // OPTIONS

    @Override
    public Set<HttpMethod> optionsForAllow(String url, Object... uriVariables) throws RestClientException {
        ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
        HttpHeaders headers = execute(url, HttpMethod.OPTIONS, null, headersExtractor, uriVariables);
        return (headers != null ? headers.getAllow() : Collections.emptySet());
    }

    @Override
    public Set<HttpMethod> optionsForAllow(String url, Map<String, ?> uriVariables) throws RestClientException {
        ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
        HttpHeaders headers = execute(url, HttpMethod.OPTIONS, null, headersExtractor, uriVariables);
        return (headers != null ? headers.getAllow() : Collections.emptySet());
    }

    @Override
    public Set<HttpMethod> optionsForAllow(URI url) throws RestClientException {
        ResponseExtractor<HttpHeaders> headersExtractor = headersExtractor();
        HttpHeaders headers = execute(url, HttpMethod.OPTIONS, null, headersExtractor);
        return (headers != null ? headers.getAllow() : Collections.emptySet());
    }
    // exchange

    @Override
    public <T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity, Class<T> responseType, Object... uriVariables) throws RestClientException {
        RequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
        ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
        return nonNull(execute(url, method, requestCallback, responseExtractor, uriVariables));
    }

    @Override
    public <T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity, Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {
        RequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
        ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
        return nonNull(execute(url, method, requestCallback, responseExtractor, uriVariables));
    }

    @Override
    public <T> ResponseEntity<T> exchange(URI url, HttpMethod method, @Nullable HttpEntity<?> requestEntity, Class<T> responseType) throws RestClientException {
        RequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
        ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
        return nonNull(execute(url, method, requestCallback, responseExtractor));
    }

    @Override
    public <T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType, Object... uriVariables) throws RestClientException {
        Type type = responseType.getType();
        RequestCallback requestCallback = httpEntityCallback(requestEntity, type);
        ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
        return nonNull(execute(url, method, requestCallback, responseExtractor, uriVariables));
    }

    @Override
    public <T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType, Map<String, ?> uriVariables) throws RestClientException {
        Type type = responseType.getType();
        RequestCallback requestCallback = httpEntityCallback(requestEntity, type);
        ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
        return nonNull(execute(url, method, requestCallback, responseExtractor, uriVariables));
    }

    @Override
    public <T> ResponseEntity<T> exchange(URI url, HttpMethod method, @Nullable HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType) throws RestClientException {
        Type type = responseType.getType();
        RequestCallback requestCallback = httpEntityCallback(requestEntity, type);
        ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
        return nonNull(execute(url, method, requestCallback, responseExtractor));
    }

    @Override
    public <T> ResponseEntity<T> exchange(RequestEntity<?> requestEntity, Class<T> responseType) throws RestClientException {
        RequestCallback requestCallback = httpEntityCallback(requestEntity, responseType);
        ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
        return nonNull(doExecute(requestEntity.getUrl(), requestEntity.getMethod(), requestCallback, responseExtractor));
    }

    @Override
    public <T> ResponseEntity<T> exchange(RequestEntity<?> requestEntity, ParameterizedTypeReference<T> responseType) throws RestClientException {
        Type type = responseType.getType();
        RequestCallback requestCallback = httpEntityCallback(requestEntity, type);
        ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
        return nonNull(doExecute(requestEntity.getUrl(), requestEntity.getMethod(), requestCallback, responseExtractor));
    }
    // General execution

    @Override
    @Nullable
    public <T> T execute(String url, HttpMethod method, @Nullable RequestCallback requestCallback, @Nullable ResponseExtractor<T> responseExtractor, Object... uriVariables) throws RestClientException {
        URI expanded = getUriTemplateHandler().expand(url, uriVariables);
        return doExecute(expanded, method, requestCallback, responseExtractor);
    }

    @Override
    @Nullable
    public <T> T execute(String url, HttpMethod method, @Nullable RequestCallback requestCallback, @Nullable ResponseExtractor<T> responseExtractor, Map<String, ?> uriVariables) throws RestClientException {
        URI expanded = getUriTemplateHandler().expand(url, uriVariables);
        return doExecute(expanded, method, requestCallback, responseExtractor);
    }

    @Override
    @Nullable
    public <T> T execute(URI url, HttpMethod method, @Nullable RequestCallback requestCallback, @Nullable ResponseExtractor<T> responseExtractor) throws RestClientException {
        return doExecute(url, method, requestCallback, responseExtractor);
    }

    @Nullable
    protected <T> T doExecute(URI url, @Nullable HttpMethod method, @Nullable RequestCallback requestCallback, @Nullable ResponseExtractor<T> responseExtractor) throws RestClientException {
        Assert.notNull(url, "URI is required");
        Assert.notNull(method, "HttpMethod is required");
        ClientHttpResponse response = null;
        try {
            ClientHttpRequest request = createRequest(url, method);
            if (requestCallback != null) {
                requestCallback.doWithRequest(request);
            }
            response = request.execute();
            handleResponse(url, method, response);
            return (responseExtractor != null ? responseExtractor.extractData(response) : null);
        } catch (IOException ex) {
            String resource = url.toString();
            String query = url.getRawQuery();
            resource = (query != null ? resource.substring(0, resource.indexOf('?')) : resource);
            throw new ResourceAccessException("I/O error on " + method.name() + " request for \"" + resource + "\": " + ex.getMessage(), ex);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    protected void handleResponse(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
        ResponseErrorHandler errorHandler = getErrorHandler();
        boolean hasError = errorHandler.hasError(response);
        if (logger.isDebugEnabled()) {
            try {
                int code = response.getRawStatusCode();
                HttpStatus status = HttpStatus.resolve(code);
                logger.debug("Response " + (status != null ? status : code));
            } catch (IOException ex) {
                // ignore
            }
        }
        if (hasError) {
            errorHandler.handleError(url, method, response);
        }
    }

    public <T> RequestCallback acceptHeaderRequestCallback(Class<T> responseType) {
        return new AcceptHeaderRequestCallback(responseType);
    }

    public <T> RequestCallback httpEntityCallback(@Nullable Object requestBody) {
        return new HttpEntityRequestCallback(requestBody);
    }

    public <T> RequestCallback httpEntityCallback(@Nullable Object requestBody, Type responseType) {
        return new HttpEntityRequestCallback(requestBody, responseType);
    }

    public <T> ResponseExtractor<ResponseEntity<T>> responseEntityExtractor(Type responseType) {
        return new ResponseEntityResponseExtractor<>(responseType);
    }

    protected ResponseExtractor<HttpHeaders> headersExtractor() {
        return this.headersExtractor;
    }

    private static <T> T nonNull(@Nullable T result) {
        Assert.state(result != null, "No result");
        return result;
    }

    private class AcceptHeaderRequestCallback implements RequestCallback {

        @Nullable
        private final Type responseType;

        public AcceptHeaderRequestCallback(@Nullable Type responseType) {
            this.responseType = responseType;
        }

        @Override
        public void doWithRequest(ClientHttpRequest request) throws IOException {
            if (this.responseType != null) {
                List<MediaType> allSupportedMediaTypes = getMessageConverters().stream().filter(converter -> canReadResponse(this.responseType, converter)).flatMap(this::getSupportedMediaTypes).distinct().sorted(MediaType.SPECIFICITY_COMPARATOR).collect(Collectors.toList());
                if (logger.isDebugEnabled()) {
                    logger.debug("Accept=" + allSupportedMediaTypes);
                }
                request.getHeaders().setAccept(allSupportedMediaTypes);
            }
        }

        private boolean canReadResponse(Type responseType, HttpMessageConverter<?> converter) {
            Class<?> responseClass = (responseType instanceof Class ? (Class<?>) responseType : null);
            if (responseClass != null) {
                return converter.canRead(responseClass, null);
            } else if (converter instanceof GenericHttpMessageConverter) {
                GenericHttpMessageConverter<?> genericConverter = (GenericHttpMessageConverter<?>) converter;
                return genericConverter.canRead(responseType, null, null);
            }
            return false;
        }

        private Stream<MediaType> getSupportedMediaTypes(HttpMessageConverter<?> messageConverter) {
            return messageConverter.getSupportedMediaTypes().stream().map(mediaType -> {
                if (mediaType.getCharset() != null) {
                    return new MediaType(mediaType.getType(), mediaType.getSubtype());
                }
                return mediaType;
            });
        }

    }

    private class HttpEntityRequestCallback extends AcceptHeaderRequestCallback {

        private final HttpEntity<?> requestEntity;

        public HttpEntityRequestCallback(@Nullable Object requestBody) {
            this(requestBody, null);
        }

        public HttpEntityRequestCallback(@Nullable Object requestBody, @Nullable Type responseType) {
            super(responseType);
            if (requestBody instanceof HttpEntity) {
                this.requestEntity = (HttpEntity<?>) requestBody;
            } else if (requestBody != null) {
                this.requestEntity = new HttpEntity<>(requestBody);
            } else {
                this.requestEntity = HttpEntity.EMPTY;
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void doWithRequest(ClientHttpRequest httpRequest) throws IOException {
            super.doWithRequest(httpRequest);
            Object requestBody = this.requestEntity.getBody();
            if (requestBody == null) {
                HttpHeaders httpHeaders = httpRequest.getHeaders();
                HttpHeaders requestHeaders = this.requestEntity.getHeaders();
                if (!requestHeaders.isEmpty()) {
                    requestHeaders.forEach((key, values) -> httpHeaders.put(key, new LinkedList<>(values)));
                }
                if (httpHeaders.getContentLength() < 0) {
                    httpHeaders.setContentLength(0L);
                }
            } else {
                Class<?> requestBodyClass = requestBody.getClass();
                Type requestBodyType = (this.requestEntity instanceof RequestEntity ? ((RequestEntity<?>) this.requestEntity).getType() : requestBodyClass);
                HttpHeaders httpHeaders = httpRequest.getHeaders();
                HttpHeaders requestHeaders = this.requestEntity.getHeaders();
                MediaType requestContentType = requestHeaders.getContentType();
                for (HttpMessageConverter<?> messageConverter : getMessageConverters()) {
                    if (messageConverter instanceof GenericHttpMessageConverter) {
                        GenericHttpMessageConverter<Object> genericConverter = (GenericHttpMessageConverter<Object>) messageConverter;
                        if (genericConverter.canWrite(requestBodyType, requestBodyClass, requestContentType)) {
                            if (!requestHeaders.isEmpty()) {
                                requestHeaders.forEach((key, values) -> httpHeaders.put(key, new LinkedList<>(values)));
                            }
                            logBody(requestBody, requestContentType, genericConverter);
                            genericConverter.write(requestBody, requestBodyType, requestContentType, httpRequest);
                            return;
                        }
                    } else if (messageConverter.canWrite(requestBodyClass, requestContentType)) {
                        if (!requestHeaders.isEmpty()) {
                            requestHeaders.forEach((key, values) -> httpHeaders.put(key, new LinkedList<>(values)));
                        }
                        logBody(requestBody, requestContentType, messageConverter);
                        ((HttpMessageConverter<Object>) messageConverter).write(requestBody, requestContentType, httpRequest);
                        return;
                    }
                }
                String message = "No HttpMessageConverter for " + requestBodyClass.getName();
                if (requestContentType != null) {
                    message += " and content type \"" + requestContentType + "\"";
                }
                throw new RestClientException(message);
            }
        }

        private void logBody(Object body, @Nullable MediaType mediaType, HttpMessageConverter<?> converter) {
            if (logger.isDebugEnabled()) {
                if (mediaType != null) {
                    logger.debug("Writing [" + body + "] as \"" + mediaType + "\"");
                } else {
                    logger.debug("Writing [" + body + "] with " + converter.getClass().getName());
                }
            }
        }

    }

    private class ResponseEntityResponseExtractor<T> implements ResponseExtractor<ResponseEntity<T>> {

        @Nullable
        private final HttpMessageConverterExtractor<T> delegate;

        public ResponseEntityResponseExtractor(@Nullable Type responseType) {
            if (responseType != null && Void.class != responseType) {
                this.delegate = new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
            } else {
                this.delegate = null;
            }
        }

        @Override
        public ResponseEntity<T> extractData(ClientHttpResponse response) throws IOException {
            if (this.delegate != null) {
                T body = this.delegate.extractData(response);
                return ResponseEntity.status(response.getRawStatusCode()).headers(response.getHeaders()).body(body);
            } else {
                return ResponseEntity.status(response.getRawStatusCode()).headers(response.getHeaders()).build();
            }
        }

    }

    private static class HeadersExtractor implements ResponseExtractor<HttpHeaders> {

        @Override
        public HttpHeaders extractData(ClientHttpResponse response) {
            return response.getHeaders();
        }

    }

}
