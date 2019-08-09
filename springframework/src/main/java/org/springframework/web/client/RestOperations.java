package org.springframework.web.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.lang.Nullable;

import java.net.URI;
import java.util.Map;
import java.util.Set;

public interface RestOperations {
    // GET

    @Nullable
    <T> T getForObject(String url, Class<T> responseType, Object... uriVariables) throws RestClientException;

    @Nullable
    <T> T getForObject(String url, Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

    @Nullable
    <T> T getForObject(URI url, Class<T> responseType) throws RestClientException;

    <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Object... uriVariables) throws RestClientException;

    <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

    <T> ResponseEntity<T> getForEntity(URI url, Class<T> responseType) throws RestClientException;
    // HEAD

    HttpHeaders headForHeaders(String url, Object... uriVariables) throws RestClientException;

    HttpHeaders headForHeaders(String url, Map<String, ?> uriVariables) throws RestClientException;

    HttpHeaders headForHeaders(URI url) throws RestClientException;
    // POST

    @Nullable
    URI postForLocation(String url, @Nullable Object request, Object... uriVariables) throws RestClientException;

    @Nullable
    URI postForLocation(String url, @Nullable Object request, Map<String, ?> uriVariables) throws RestClientException;

    @Nullable
    URI postForLocation(URI url, @Nullable Object request) throws RestClientException;

    @Nullable
    <T> T postForObject(String url, @Nullable Object request, Class<T> responseType, Object... uriVariables) throws RestClientException;

    @Nullable
    <T> T postForObject(String url, @Nullable Object request, Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

    @Nullable
    <T> T postForObject(URI url, @Nullable Object request, Class<T> responseType) throws RestClientException;

    <T> ResponseEntity<T> postForEntity(String url, @Nullable Object request, Class<T> responseType, Object... uriVariables) throws RestClientException;

    <T> ResponseEntity<T> postForEntity(String url, @Nullable Object request, Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

    <T> ResponseEntity<T> postForEntity(URI url, @Nullable Object request, Class<T> responseType) throws RestClientException;
    // PUT

    void put(String url, @Nullable Object request, Object... uriVariables) throws RestClientException;

    void put(String url, @Nullable Object request, Map<String, ?> uriVariables) throws RestClientException;

    void put(URI url, @Nullable Object request) throws RestClientException;
    // PATCH

    @Nullable
    <T> T patchForObject(String url, @Nullable Object request, Class<T> responseType, Object... uriVariables) throws RestClientException;

    @Nullable
    <T> T patchForObject(String url, @Nullable Object request, Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

    @Nullable
    <T> T patchForObject(URI url, @Nullable Object request, Class<T> responseType) throws RestClientException;
    // DELETE

    void delete(String url, Object... uriVariables) throws RestClientException;

    void delete(String url, Map<String, ?> uriVariables) throws RestClientException;

    void delete(URI url) throws RestClientException;
    // OPTIONS

    Set<HttpMethod> optionsForAllow(String url, Object... uriVariables) throws RestClientException;

    Set<HttpMethod> optionsForAllow(String url, Map<String, ?> uriVariables) throws RestClientException;

    Set<HttpMethod> optionsForAllow(URI url) throws RestClientException;
    // exchange

    <T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity, Class<T> responseType, Object... uriVariables) throws RestClientException;

    <T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity, Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

    <T> ResponseEntity<T> exchange(URI url, HttpMethod method, @Nullable HttpEntity<?> requestEntity, Class<T> responseType) throws RestClientException;

    <T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType, Object... uriVariables) throws RestClientException;

    <T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType, Map<String, ?> uriVariables) throws RestClientException;

    <T> ResponseEntity<T> exchange(URI url, HttpMethod method, @Nullable HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType) throws RestClientException;

    <T> ResponseEntity<T> exchange(RequestEntity<?> requestEntity, Class<T> responseType) throws RestClientException;

    <T> ResponseEntity<T> exchange(RequestEntity<?> requestEntity, ParameterizedTypeReference<T> responseType) throws RestClientException;
    // General execution

    @Nullable
    <T> T execute(String url, HttpMethod method, @Nullable RequestCallback requestCallback, @Nullable ResponseExtractor<T> responseExtractor, Object... uriVariables) throws RestClientException;

    @Nullable
    <T> T execute(String url, HttpMethod method, @Nullable RequestCallback requestCallback, @Nullable ResponseExtractor<T> responseExtractor, Map<String, ?> uriVariables) throws RestClientException;

    @Nullable
    <T> T execute(URI url, HttpMethod method, @Nullable RequestCallback requestCallback, @Nullable ResponseExtractor<T> responseExtractor) throws RestClientException;

}
